package com.example.ir

import com.example.data.model.ACMode
import com.example.data.model.AirconState
import com.example.data.model.FanSpeed

object Glp09PortIrProtocol {
    const val CARRIER_FREQUENCY_HZ = 38000

    // Pulse & Space timings in microseconds
    private const val HDR_MARK = 9000
    private const val HDR_SPACE = 4500
    private const val BIT_MARK = 560
    private const val ZERO_SPACE = 560
    private const val ONE_SPACE = 1680
    private const val STOP_MARK = 560
    private const val STOP_SPACE = 10000

    /**
     * Builds the 5-byte payload for GLP-09PORT logic controller
     */
    fun buildPacket(state: AirconState, isSensorFlushSequence: Boolean = false): ByteArray {
        val b0 = 0xC3.toByte() // GLP-09PORT Header ID

        // Byte 1: Power, Mode, Fan Speed
        val pwrBit = if (state.power) 1 else 0
        val modeVal = when (state.mode) {
            ACMode.COOL -> 0
            ACMode.DRY -> 1
            ACMode.FAN_ONLY -> 2
            ACMode.ECO -> 3
            ACMode.HEAT -> 4
        }
        val fanVal = when (state.fanSpeed) {
            FanSpeed.AUTO -> 0
            FanSpeed.LOW -> 1
            FanSpeed.MEDIUM -> 2
            FanSpeed.HIGH -> 3
        }
        val b1 = (pwrBit or ((modeVal and 0x07) shl 1) or (fanVal shl 4)).toByte()

        // Byte 2: Target Temp (16..31 offset by 16 = 0..15), Swing, Turbo, Sleep
        val tempVal = (state.targetTemp.coerceIn(16, 31) - 16) and 0x0F
        val swingVal = if (state.swing) (1 shl 4) else 0
        val turboVal = if (state.turbo) (1 shl 5) else 0
        val sleepVal = if (state.sleep) (1 shl 6) else 0
        val b2 = (tempVal or swingVal or turboVal or sleepVal).toByte()

        // Byte 3: Special triggers: Sensor Flush / Purge flag, Display LED, Timer
        val flushVal = if (isSensorFlushSequence || state.isFlushingSensor) 1 else 0
        val ledVal = if (state.displayLed) (1 shl 1) else 0
        val timerVal = (state.timerHours.coerceIn(0, 24) shl 2)
        val b3 = (flushVal or ledVal or timerVal).toByte()

        // Byte 4: Checksum
        val sum = ((b0.toInt() and 0xFF) + (b1.toInt() and 0xFF) + (b2.toInt() and 0xFF) + (b3.toInt() and 0xFF)) and 0xFF
        val b4 = sum.toByte()

        return byteArrayOf(b0, b1, b2, b3, b4)
    }

    /**
     * Converts a 5-byte packet to standard ConsumerIrManager microsecond pulse/space array.
     */
    fun packetToMicrosecondPattern(packet: ByteArray): IntArray {
        // Header (2) + 40 bits * 2 (80) + Stop mark (1) = 83 entries
        val timingList = ArrayList<Int>(85)

        // Header mark and space
        timingList.add(HDR_MARK)
        timingList.add(HDR_SPACE)

        // Transmit each byte LSB first (standard NEC/Aircon transmission)
        for (byte in packet) {
            var value = byte.toInt() and 0xFF
            for (bit in 0 until 8) {
                timingList.add(BIT_MARK)
                if ((value and 1) == 1) {
                    timingList.add(ONE_SPACE)
                } else {
                    timingList.add(ZERO_SPACE)
                }
                value = value shr 1
            }
        }

        // Stop mark and trailing gap
        timingList.add(STOP_MARK)
        timingList.add(STOP_SPACE)

        return timingList.toIntArray()
    }

    /**
     * Formats bytes as uppercase hex string: "C3 21 06 02 F0"
     */
    fun toHexString(bytes: ByteArray): String {
        return bytes.joinToString(" ") { String.format("%02X", it) }
    }

    /**
     * Formats for Wi-Fi IR gateway (Tasmota / ESP32 IR Blaster JSON)
     */
    fun toWifiGatewayPayload(state: AirconState, isSensorFlushSequence: Boolean = false): String {
        val packet = buildPacket(state, isSensorFlushSequence)
        val hex = packet.joinToString("") { String.format("%02X", it) }
        return """{"Protocol":"GLP_09PORT","Bits":40,"Data":"0x$hex","Temp":${state.targetTemp},"Mode":"${state.mode.shortCode}","Fan":"${state.fanSpeed.displayName}"}"""
    }
}
