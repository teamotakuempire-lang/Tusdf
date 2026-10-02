package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CoolBlue
import com.example.ui.theme.DryPurple
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.FanTeal
import com.example.ui.theme.TurboOrange

enum class ACMode(
    val displayName: String,
    val shortCode: String,
    val iconEmoji: String,
    val accentColor: Color,
    val protocolCode: Int
) {
    COOL("Cooling", "COOL", "❄️", CoolBlue, 0),
    HEAT("Heating", "HEAT", "☀️", TurboOrange, 4),
    DRY("Dehumidify", "DRY", "💧", DryPurple, 1),
    FAN_ONLY("Fan Only", "FAN", "🌀", FanTeal, 2),
    ECO("Eco Mode", "ECO", "🌱", EcoGreen, 3)
}

enum class FanSpeed(
    val displayName: String,
    val speedLevel: Int,
    val protocolCode: Int
) {
    AUTO("Auto", 0, 0),
    LOW("Low", 1, 1),
    MEDIUM("Medium", 2, 2),
    HIGH("High", 3, 3)
}

enum class TransmitterMode(
    val title: String,
    val subtitle: String
) {
    BUILTIN_IR("Internal Phone IR Emitter", "Direct hardware IR diode (ConsumerIrManager)"),
    WIFI_GATEWAY("Wi-Fi IR Bridge / Blaster", "Tasmota / ESP32 / Broadlink HTTP gateway"),
    SIMULATION("Loopback Virtual Blaster", "Emulated transmission with waveform visualizer")
}

enum class SensorSource(
    val title: String,
    val shortLabel: String,
    val iconEmoji: String,
    val description: String
) {
    TUYA_SENSOR("Tuya / Smart Life Temp Sensor", "Tuya Sensor", "📡", "Wireless Tuya / Zigbee room sensor across the room"),
    BLASTER_BATTERY_SENSOR("IR Blaster Battery Sensor", "Blaster Sensor", "🔋", "IR Blaster hardware calibrated thermistor & battery monitor"),
    AC_INTERNAL_INTAKE("AC Intake Thermistor", "AC Intake", "🌀", "Internal GLP-09PORT thermistor (prone to siphon lock)")
}
