package com.example.sensor

import com.example.data.model.AirconState
import kotlin.math.abs

data class SensorReading(
    val timestampMs: Long,
    val temperatureCelsius: Double,
    val isCompressorActive: Boolean
)

data class StuckSensorReport(
    val isStuck: Boolean,
    val stuckMinutes: Long,
    val variance: Double,
    val reason: String,
    val recommendedAction: String
)

class StuckSensorWatchdog {
    // Rolling history of recent sensor readings (last 60 minutes)
    private val readings = mutableListOf<SensorReading>()
    
    // Configurable thresholds
    private val minMinutesForStuckDetection = 35L
    private val maxFluctuationThreshold = 0.15 // If change < 0.15°C over 35+ mins while running
    
    // Simulated sensor behavior state
    private var isSimulatingStuck: Boolean = false
    private var simulatedStuckTemp: Double = 18.2

    @Synchronized
    fun recordReading(temp: Double, isRunning: Boolean, nowMs: Long = System.currentTimeMillis()) {
        readings.add(SensorReading(nowMs, temp, isRunning))
        // Prune older than 2 hours
        val cutoff = nowMs - (120 * 60 * 1000L)
        readings.removeAll { it.timestampMs < cutoff }
    }

    @Synchronized
    fun setSimulateStuck(stuck: Boolean, fixedTemp: Double = 18.2) {
        isSimulatingStuck = stuck
        simulatedStuckTemp = fixedTemp
        if (stuck) {
            val now = System.currentTimeMillis()
            readings.clear()
            // Inject 45 minutes of flat readings
            for (i in 45 downTo 0) {
                readings.add(SensorReading(now - (i * 60 * 1000L), fixedTemp, true))
            }
        }
    }

    @Synchronized
    fun analyze(state: AirconState, nowMs: Long = System.currentTimeMillis()): StuckSensorReport {
        if (isSimulatingStuck) {
            return StuckSensorReport(
                isStuck = true,
                stuckMinutes = 48,
                variance = 0.02,
                reason = "Thermal siphon lock: Sensor flatlined at ${simulatedStuckTemp}°C for >45 min while cooling",
                recommendedAction = "Execute 90-second High-Velocity Thermal Siphon Flush to clear trapped intake air."
            )
        }

        if (readings.size < 6) {
            return StuckSensorReport(
                isStuck = false,
                stuckMinutes = 0,
                variance = 0.5,
                reason = "Calibrating baseline readings...",
                recommendedAction = "Normal operation"
            )
        }

        val windowCutoff = nowMs - (minMinutesForStuckDetection * 60 * 1000L)
        val windowReadings = readings.filter { it.timestampMs >= windowCutoff }

        if (windowReadings.size < 5) {
            return StuckSensorReport(
                isStuck = false,
                stuckMinutes = 0,
                variance = 0.3,
                reason = "Insufficient window history",
                recommendedAction = "Normal operation"
            )
        }

        val temps = windowReadings.map { it.temperatureCelsius }
        val minTemp = temps.minOrNull() ?: 0.0
        val maxTemp = temps.maxOrNull() ?: 0.0
        val delta = abs(maxTemp - minTemp)

        val runningCount = windowReadings.count { it.isCompressorActive }
        val wasRunningConsistently = runningCount >= (windowReadings.size * 0.7)

        val durationSpanMinutes = (windowReadings.last().timestampMs - windowReadings.first().timestampMs) / (60 * 1000L)

        if (wasRunningConsistently && delta < maxFluctuationThreshold && durationSpanMinutes >= minMinutesForStuckDetection) {
            return StuckSensorReport(
                isStuck = true,
                stuckMinutes = durationSpanMinutes,
                variance = delta,
                reason = "Thermal pocketing: Internal thermistor stationary at %.1f°C for %d mins".format(temps.last(), durationSpanMinutes),
                recommendedAction = "Run Thermal Siphon Flush to purge intake air, or apply a calibration offset."
            )
        }

        return StuckSensorReport(
            isStuck = false,
            stuckMinutes = 0,
            variance = delta,
            reason = "Sensor dynamic: %.1f°C delta over %d mins".format(delta, durationSpanMinutes),
            recommendedAction = "Normal active feedback"
        )
    }

    @Synchronized
    fun resetAfterFlush() {
        isSimulatingStuck = false
        readings.clear()
        val now = System.currentTimeMillis()
        // Inject fresh post-flush dynamic readings
        readings.add(SensorReading(now, 24.5, true))
    }
}
