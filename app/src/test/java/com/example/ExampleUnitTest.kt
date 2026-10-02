package com.example

import com.example.data.model.ACMode
import com.example.data.model.AirconState
import com.example.data.model.FanSpeed
import com.example.data.model.ScheduleOverride
import com.example.data.model.SensorSource
import com.example.ir.Glp09PortIrProtocol
import com.example.sensor.StuckSensorWatchdog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testGlp09PortIrProtocolPacketGeneration() {
        val state = AirconState(
            power = true,
            mode = ACMode.COOL,
            targetTemp = 22,
            fanSpeed = FanSpeed.AUTO,
            swing = true,
            turbo = false
        )

        val packet = Glp09PortIrProtocol.buildPacket(state)
        assertEquals(5, packet.size)
        // Byte 0 should be 0xC3 (Logic aircon brand sync header)
        assertEquals(0xC3.toByte(), packet[0])

        // Verify checksum is sum of bytes 0..3 modulo 256
        val b0 = packet[0].toInt() and 0xFF
        val b1 = packet[1].toInt() and 0xFF
        val b2 = packet[2].toInt() and 0xFF
        val b3 = packet[3].toInt() and 0xFF
        val expectedChecksum = ((b0 + b1 + b2 + b3) and 0xFF).toByte()
        assertEquals(expectedChecksum, packet[4])

        // Verify timing pattern generation
        val pattern = Glp09PortIrProtocol.packetToMicrosecondPattern(packet)
        assertTrue(pattern.size > 80)
        assertEquals(9000, pattern[0]) // Header mark
        assertEquals(4500, pattern[1]) // Header space
    }

    @Test
    fun testGlp09PortHeatModePacketGeneration() {
        // Test user's explicitly requested HEAT mode
        val heatState = AirconState(
            power = true,
            mode = ACMode.HEAT,
            targetTemp = 25,
            fanSpeed = FanSpeed.AUTO
        )

        val packet = Glp09PortIrProtocol.buildPacket(heatState)
        assertEquals(5, packet.size)
        assertEquals(0xC3.toByte(), packet[0])

        // Check byte 1 contains heat mode code (4 shl 1 = 8)
        val b1 = packet[1].toInt() and 0xFF
        val modeBits = (b1 shr 1) and 0x07
        assertEquals(4, modeBits) // ACMode.HEAT protocolCode = 4

        // Verify checksum integrity
        val b0 = packet[0].toInt() and 0xFF
        val b2 = packet[2].toInt() and 0xFF
        val b3 = packet[3].toInt() and 0xFF
        val expectedChecksum = ((b0 + b1 + b2 + b3) and 0xFF).toByte()
        assertEquals(expectedChecksum, packet[4])
    }

    @Test
    fun testSensorSourceAndDualThresholdLogic() {
        // Test user's request: "measure the tunya temp it blasters temperature and then to put on at 24 c and of at 25 c or it's own battery one calibrated option"
        val stateTuya = AirconState(
            activeSensorSource = SensorSource.TUYA_SENSOR,
            tuyaTemp = 23.8,
            blasterBatteryTemp = 24.2,
            blasterCalibratedOffset = -0.4,
            dualCutInTemp = 24.0,
            dualCutOutTemp = 25.0
        )
        // Selected source is Tuya -> effectiveControlTemp should equal tuyaTemp (23.8°C)
        assertEquals(23.8, stateTuya.effectiveControlTemp, 0.001)

        // Below or at 24.0°C cut-in -> heat should trigger ON
        assertTrue(stateTuya.effectiveControlTemp <= stateTuya.dualCutInTemp)

        // Switch source to Blaster Battery with calibration (-0.4°C)
        val stateBlaster = stateTuya.copy(
            activeSensorSource = SensorSource.BLASTER_BATTERY_SENSOR
        )
        // 24.2 - 0.4 = 23.8°C
        assertEquals(23.8, stateBlaster.effectiveControlTemp, 0.001)

        // Now test warm condition: 25.2°C -> should trigger cut-out (OFF)
        val warmState = stateTuya.copy(tuyaTemp = 25.2)
        assertTrue(warmState.effectiveControlTemp >= warmState.dualCutOutTemp)
    }

    @Test
    fun testStuckSensorWatchdogDetection() {
        val watchdog = StuckSensorWatchdog()
        val now = System.currentTimeMillis()

        // Inject 40 minutes of flat readings
        for (i in 40 downTo 0) {
            watchdog.recordReading(18.2, isRunning = true, nowMs = now - (i * 60 * 1000L))
        }

        val dummyState = AirconState(ambientTemp = 18.2, power = true)
        val report = watchdog.analyze(dummyState, now)

        assertTrue("Watchdog should flag flatlined readings as stuck", report.isStuck)
        assertTrue(report.stuckMinutes >= 35)

        // After flush reset, it should clear
        watchdog.resetAfterFlush()
        val resetReport = watchdog.analyze(dummyState, now)
        assertFalse(resetReport.isStuck)
    }

    @Test
    fun testScheduleOverrideCalculations() {
        val now = 1000000L
        val override = ScheduleOverride(
            id = "test_override",
            name = "1h Boost Chill",
            targetTemp = 18,
            mode = ACMode.COOL,
            fanSpeed = FanSpeed.HIGH,
            startedAtEpochMs = now,
            durationMinutes = 60
        )

        assertFalse(override.isExpired(now + 1000L))
        assertEquals(3600L, override.remainingSeconds(now))
        assertTrue(override.isExpired(now + (61 * 60 * 1000L)))
    }
}
