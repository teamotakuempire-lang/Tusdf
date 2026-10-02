package com.example.data.model

data class AirconState(
    val power: Boolean = true,
    val mode: ACMode = ACMode.COOL,
    val targetTemp: Int = 22, // 16 to 31 °C
    val ambientTemp: Double = 25.4,
    val humidity: Int = 54,
    val fanSpeed: FanSpeed = FanSpeed.AUTO,
    val swing: Boolean = false,
    val turbo: Boolean = false,
    val sleep: Boolean = false,
    val displayLed: Boolean = true,
    val timerHours: Int = 0,
    
    // Sensor Source Selection
    val activeSensorSource: SensorSource = SensorSource.TUYA_SENSOR,
    
    // External Tuya / Smart Life Temperature Sensor
    val tuyaTemp: Double = 23.8,
    val tuyaHumidity: Int = 51,
    val tuyaDeviceName: String = "Tuya Zigbee Living Room Probe",
    val tuyaDeviceId: String = "bf7382901a88",
    val tuyaEndpointUrl: String = "http://192.168.1.150/tuya/sensor",
    val tuyaLastSyncEpochMs: Long = System.currentTimeMillis(),
    
    // IR Blaster Hardware Battery & Calibrated Onboard Sensor
    val blasterBatteryLevel: Int = 89, // Percentage (e.g. 89%)
    val blasterBatteryVoltage: Double = 3.92, // Volts
    val blasterBatteryTemp: Double = 24.2, // Onboard thermistor reading
    val blasterCalibratedOffset: Double = -0.4, // User calibration
    
    // Dual-Threshold Hysteresis Automation (User requested: ON at 24°C, OFF at 25°C)
    val dualThresholdActive: Boolean = true,
    val dualCutInTemp: Double = 24.0, // Turn ON threshold
    val dualCutOutTemp: Double = 25.0, // Turn OFF threshold
    val dualTargetMode: ACMode = ACMode.HEAT,
    val dualTargetFanSpeed: FanSpeed = FanSpeed.AUTO,
    
    // Internal Sensor Health & Diagnostics
    val sensorOffset: Double = 0.0, // Calibrated offset (-5.0 to +5.0 °C)
    val isSensorStuck: Boolean = false,
    val stuckDurationMinutes: Long = 0,
    val lastSensorReadingTimeMs: Long = System.currentTimeMillis(),
    val isFlushingSensor: Boolean = false,
    val flushSecondsRemaining: Int = 0,
    
    // Schedule Overrides
    val activeOverride: ScheduleOverride? = null,
    
    // Hardware & IR Blaster
    val transmitterMode: TransmitterMode = TransmitterMode.BUILTIN_IR,
    val wifiGatewayUrl: String = "http://192.168.1.120/cm?cmnd=",
    val lastTransmittedCommand: String = "READY",
    val lastTransmittedHex: String = "C3 20 06 00 E9",
    val hasBuiltInIr: Boolean = false,
    val lastTransmitSuccess: Boolean = true
) {
    // Returns the active temperature according to user's selected sensor source
    val effectiveControlTemp: Double
        get() = when (activeSensorSource) {
            SensorSource.TUYA_SENSOR -> tuyaTemp
            SensorSource.BLASTER_BATTERY_SENSOR -> blasterBatteryTemp + blasterCalibratedOffset
            SensorSource.AC_INTERNAL_INTAKE -> ambientTemp + sensorOffset
        }

    val effectiveAmbientTemp: Double
        get() = (ambientTemp + sensorOffset)
}
