package com.example.data.model

enum class ConditionType(
    val title: String,
    val unit: String
) {
    TEMP_ABOVE("Ambient Temp Rises Above", "°C"),
    TEMP_BELOW("Ambient Temp Drops Below", "°C"),
    HUMIDITY_ABOVE("Room Humidity Exceeds", "%"),
    STUCK_SENSOR_TRIGGER("Stuck Sensor Detected Duration", "min"),
    TIME_WINDOW("Daily Time Window", "")
}

data class AutomationRule(
    val id: String,
    val title: String,
    val description: String,
    val conditionType: ConditionType,
    val thresholdValue: Double,
    val timeStart: String = "08:00",
    val timeEnd: String = "22:00",
    val activeDays: List<Int> = listOf(1, 2, 3, 4, 5, 6, 7), // 1=Mon .. 7=Sun
    val actionPower: Boolean = true,
    val actionTemp: Int = 22,
    val actionMode: ACMode = ACMode.COOL,
    val actionFanSpeed: FanSpeed = FanSpeed.AUTO,
    val triggerSensorFlush: Boolean = false,
    val isEnabled: Boolean = true,
    val lastTriggeredEpochMs: Long? = null
)
