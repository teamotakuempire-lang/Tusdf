package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SensorSource
import com.example.ui.theme.AlertRed
import com.example.ui.theme.AlertRedContainer
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.SurfaceCardBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TurboOrange
import com.example.ui.viewmodel.AirconViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SensorResetScreen(
    viewModel: AirconViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val stuckReport by viewModel.stuckReport.collectAsState()
    val sensorLogs by viewModel.sensorLogs.collectAsState()
    val scrollState = rememberScrollState()

    var showRezeroDialog by remember { mutableStateOf(false) }
    var referenceTempInput by remember { mutableStateOf("24.5") }
    var showTuyaConfigDialog by remember { mutableStateOf(false) }
    var tuyaEndpointInput by remember(state.tuyaEndpointUrl) { mutableStateOf(state.tuyaEndpointUrl) }

    // Fan animation when flushing
    val infiniteTransition = rememberInfiniteTransition(label = "fan_rotation")
    val fanRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "fan_rotation"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        // Title Bar
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Sensor Calibration & Reset",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Tuya probe, blaster battery thermistor & siphon lock reset",
                style = MaterialTheme.typography.bodySmall,
                color = CyanPrimary
            )
        }

        // ==========================================
        // 1. Primary Sensor Source Selector
        // ==========================================
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "ACTIVE SENSOR CONTROLLING AC",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                SensorSource.values().forEach { source ->
                    val isSelected = state.activeSensorSource == source
                    val currentTempDisplay = when (source) {
                        SensorSource.TUYA_SENSOR -> "${String.format("%.1f°C", state.tuyaTemp)} (${state.tuyaHumidity}% RH)"
                        SensorSource.BLASTER_BATTERY_SENSOR -> "${String.format("%.1f°C", state.blasterBatteryTemp + state.blasterCalibratedOffset)} [${state.blasterBatteryLevel}% Batt]"
                        SensorSource.AC_INTERNAL_INTAKE -> "${String.format("%.1f°C", state.effectiveAmbientTemp)} [Chassis]"
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) Color(0xFF1E3554) else Color(0xFF101B2B),
                        border = BorderStroke(1.dp, if (isSelected) CyanPrimary else Color(0xFF1E2D48)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { viewModel.setActiveSensorSource(source) }
                            .testTag("select_sensor_${source.name.lowercase()}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(source.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = source.title,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFFCBD5E1)
                                    )
                                    Text(
                                        text = source.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFF94A3B8),
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            Text(
                                text = currentTempDisplay,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) CyanPrimary else Color(0xFF64748B)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 2. Tuya / Smart Life External Sensor Integration
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("📡", fontSize = 20.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Tuya / Smart Life Temp Sensor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Device: ${state.tuyaDeviceName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }

                    FilledTonalButton(
                        onClick = { viewModel.syncTuyaSensor() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1E2D48),
                            contentColor = CyanPrimary
                        )
                    ) {
                        Icon(Icons.Default.Sync, contentDescription = "Sync", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sync", fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF101C2E),
                        modifier = Modifier.weight(1f).padding(end = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Tuya Temp", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                String.format("%.1f°C", state.tuyaTemp),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = CyanPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF101C2E),
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Tuya Humidity", fontSize = 10.sp, color = Color(0xFF94A3B8))
                            Text(
                                "${state.tuyaHumidity}% RH",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Test adjuster for Tuya Temp (e.g. test 23.5°C to 25.5°C triggers)
                Text(
                    text = "Simulate / Test Tuya Reading (verify 24°C ON / 25°C OFF automation):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { viewModel.setTuyaTempManual(23.5) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("23.5°C (ON)", fontSize = 10.sp, color = TurboOrange)
                    }

                    OutlinedButton(
                        onClick = { viewModel.setTuyaTempManual(24.5) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("24.5°C (Mid)", fontSize = 10.sp, color = CyanPrimary)
                    }

                    OutlinedButton(
                        onClick = { viewModel.setTuyaTempManual(25.5) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("25.5°C (OFF)", fontSize = 10.sp, color = EcoGreen)
                    }
                }
            }
        }

        // ==========================================
        // 3. IR Blaster Battery Sensor & Calibrated Thermistor
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BatteryChargingFull,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "IR Blaster Hardware Battery & Sensor",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Battery: ${state.blasterBatteryLevel}% (${state.blasterBatteryVoltage}V Li-ion)",
                                style = MaterialTheme.typography.bodySmall,
                                color = EcoGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Raw Blaster Probe", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(
                            String.format("%.1f°C", state.blasterBatteryTemp),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Column {
                        Text("Blaster Offset", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(
                            "${if (state.blasterCalibratedOffset >= 0) "+${state.blasterCalibratedOffset}" else "${state.blasterCalibratedOffset}"}°C",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = CyanPrimary
                        )
                    }

                    Column {
                        Text("Calibrated Value", fontSize = 10.sp, color = Color(0xFF94A3B8))
                        Text(
                            String.format("%.1f°C", state.blasterBatteryTemp + state.blasterCalibratedOffset),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = EcoGreen
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Blaster Hardware Calibration Offset:",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8)
                )

                Slider(
                    value = state.blasterCalibratedOffset.toFloat(),
                    onValueChange = { viewModel.setBlasterCalibratedOffset(it.toDouble()) },
                    valueRange = -5f..5f,
                    steps = 99,
                    colors = SliderDefaults.colors(
                        thumbColor = EcoGreen,
                        activeTrackColor = EcoGreen,
                        inactiveTrackColor = Color(0xFF1E2D48)
                    ),
                    modifier = Modifier.testTag("blaster_offset_slider")
                )
            }
        }

        // ==========================================
        // 4. Stuck AC Intake Thermistor & 90s Flush Reset
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (state.isSensorStuck) AlertRedContainer else SurfaceDark
            ),
            border = BorderStroke(
                1.dp,
                if (state.isSensorStuck) AlertRed else SurfaceCardBorderDark
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (state.isSensorStuck) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (state.isSensorStuck) AmberWarning else EcoGreen,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (state.isSensorStuck) "AC INTAKE SENSOR STUCK" else "AC INTAKE THERMISTOR",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (state.isSensorStuck) AmberWarning else EcoGreen
                        )
                    }

                    Text(
                        text = String.format("%.1f°C", state.effectiveAmbientTemp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = if (state.isSensorStuck) stuckReport.reason else "Watchdog active. If cold air gets trapped around the intake bulb during cooling, the 90s Thermal Siphon Flush will purge the pocket.",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (state.isSensorStuck) Color(0xFFFCA5A5) else Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (state.isFlushingSensor) {
                    val progress = (90 - state.flushSecondsRemaining) / 90f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = CyanPrimary,
                        trackColor = Color(0xFF1E2D48)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Purging air pocket... ${state.flushSecondsRemaining}s left",
                            style = MaterialTheme.typography.labelSmall,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )

                        OutlinedButton(
                            onClick = { viewModel.cancelThermalFlush() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AlertRed),
                            border = BorderStroke(1.dp, AlertRed),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Abort", fontSize = 11.sp)
                        }
                    }
                } else {
                    ElevatedButton(
                        onClick = { viewModel.startThermalSiphonFlush() },
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = CyanPrimary,
                            contentColor = Color(0xFF00325B)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("start_siphon_flush_btn")
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Execute 90s Thermal Siphon Flush", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Micro-Cycle Power Sequence
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                FilledTonalButton(
                    onClick = { viewModel.executeHardSensorResetSequence() },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1E2D48),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("hard_reset_sequence_btn")
                ) {
                    Icon(Icons.Default.Power, contentDescription = null, tint = TurboOrange, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Micro-Cycle IR Reset Sequence", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Text("Power Off -> 2.5s -> High Blow -> Re-arm", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
