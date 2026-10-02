package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.ModeFanOff
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ACMode
import com.example.data.model.SensorSource
import com.example.data.model.TransmitterMode
import com.example.ui.components.ClimateWheel
import com.example.ui.components.FanSpeedSelector
import com.example.ui.components.ModeSelector
import com.example.ui.components.ScheduleOverrideBanner
import com.example.ui.components.StuckSensorBanner
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.SurfaceCardBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TurboOrange
import com.example.ui.viewmodel.AirconViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: AirconViewModel,
    onNavigateToDiagnostics: () -> Unit,
    onNavigateToOverrides: () -> Unit,
    onNavigateToRemote: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val userEmail by viewModel.userEmail.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showCloudBackupDialog by remember { mutableStateOf(false) }
    var emailInput by remember(userEmail) { mutableStateOf(userEmail) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "GLP-09PORT",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2D48),
                        border = BorderStroke(1.dp, Color(0xFF2E4166))
                    ) {
                        Text(
                            text = if (state.mode == ACMode.HEAT) "HEAT & COOL" else "LOGIC",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (state.mode == ACMode.HEAT) TurboOrange else CyanPrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                
                // Hardware IR / Wi-Fi Blaster pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val (statusColor, statusText) = when (state.transmitterMode) {
                        TransmitterMode.BUILTIN_IR -> if (state.hasBuiltInIr) Pair(EcoGreen, "Phone Hardware IR (38kHz)") else Pair(Color(0xFFF59E0B), "IR Emitter Not Present (Sim)")
                        TransmitterMode.WIFI_GATEWAY -> Pair(CyanPrimary, "Wi-Fi IR Bridge")
                        TransmitterMode.SIMULATION -> Pair(Color(0xFF94A3B8), "Virtual IR Loopback")
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Cloud Drive / Gmail Export Button
                FilledIconButton(
                    onClick = { showCloudBackupDialog = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color(0xFF1E2D48),
                        contentColor = CyanPrimary
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("cloud_backup_header_btn")
                ) {
                    if (isCloudSyncing) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = CyanPrimary)
                    } else {
                        Icon(
                            imageVector = Icons.Default.CloudUpload,
                            contentDescription = "Save app to Google Drive and Gmail",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Power Toggle Button
                FilledIconButton(
                    onClick = { viewModel.togglePower() },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = if (state.power) (if (state.mode == ACMode.HEAT) TurboOrange else CyanPrimary) else Color(0xFF1E2D48),
                        contentColor = if (state.power) Color(0xFF00325B) else Color(0xFF64748B)
                    ),
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("power_toggle_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Google Drive & Gmail Cloud Export Banner
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color(0xFF0F1E33),
            border = BorderStroke(1.dp, Color(0xFF1F3A5F)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clickable { showCloudBackupDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyanPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("GOOGLE DRIVE & GMAIL APK EXPORT", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                        Text("$userEmail • GLP09PORT_Aircon.apk", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilledTonalButton(
                        onClick = { viewModel.backupToGoogleDrive() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E3554), contentColor = CyanPrimary),
                        modifier = Modifier.testTag("save_to_drive_btn")
                    ) {
                        Text("APK to Drive", fontSize = 11.sp)
                    }

                    FilledTonalButton(
                        onClick = { viewModel.sendBackupToGmail() },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF1E3554), contentColor = EcoGreen),
                        modifier = Modifier.testTag("send_to_gmail_btn")
                    ) {
                        Text("APK to Gmail", fontSize = 11.sp)
                    }
                }
            }
        }

        // Stuck Sensor Alert Banner
        StuckSensorBanner(
            isSensorStuck = state.isSensorStuck,
            stuckMinutes = state.stuckDurationMinutes,
            isFlushing = state.isFlushingSensor,
            flushSecondsRemaining = state.flushSecondsRemaining,
            onStartFlush = { viewModel.startThermalSiphonFlush() },
            onCancelFlush = { viewModel.cancelThermalFlush() },
            onOpenDiagnostics = onNavigateToDiagnostics
        )

        // Active Schedule Override Banner
        ScheduleOverrideBanner(
            override = state.activeOverride,
            onExtendOverride = { viewModel.extendOverride(30) },
            onCancelOverride = { viewModel.cancelOverride() }
        )

        // ==========================================
        // Sensor Source Selector (Tuya vs Blaster Battery vs Intake)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "TEMPERATURE REFERENCE SOURCE",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )

                    if (state.activeSensorSource == SensorSource.TUYA_SENSOR) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF1E2D48),
                            modifier = Modifier.clickable { viewModel.syncTuyaSensor() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Sync, contentDescription = "Sync", tint = CyanPrimary, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Sync Tuya", fontSize = 10.sp, color = CyanPrimary)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    SensorSource.values().forEach { source ->
                        val isSelected = state.activeSensorSource == source
                        val tempVal = when (source) {
                            SensorSource.TUYA_SENSOR -> String.format("%.1f°C", state.tuyaTemp)
                            SensorSource.BLASTER_BATTERY_SENSOR -> String.format("%.1f°C", state.blasterBatteryTemp + state.blasterCalibratedOffset)
                            SensorSource.AC_INTERNAL_INTAKE -> String.format("%.1f°C", state.effectiveAmbientTemp)
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) (if (state.mode == ACMode.HEAT) TurboOrange.copy(alpha = 0.25f) else CyanPrimary.copy(alpha = 0.2f)) else Color(0xFF1E2D48))
                                .border(1.dp, if (isSelected) (if (state.mode == ACMode.HEAT) TurboOrange else CyanPrimary) else Color.Transparent, RoundedCornerShape(8.dp))
                                .clickable { viewModel.setActiveSensorSource(source) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "${source.iconEmoji} ${source.shortLabel}",
                                    fontSize = 10.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color(0xFF94A3B8)
                                )
                                Text(
                                    text = tempVal,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) (if (state.mode == ACMode.HEAT) TurboOrange else CyanPrimary) else Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }

        // ==========================================
        // Dual-Threshold Hysteresis Thermostat (24°C ON / 25°C OFF)
        // ==========================================
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF122033)),
            border = BorderStroke(1.dp, if (state.dualThresholdActive) (if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary) else SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Thermostat,
                            contentDescription = null,
                            tint = if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text(
                                text = "DUAL-THRESHOLD THERMOSTAT",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Auto ${state.dualTargetMode.shortCode}: ON @ ${state.dualCutInTemp.toInt()}°C  •  OFF @ ${state.dualCutOutTemp.toInt()}°C",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Switch(
                        checked = state.dualThresholdActive,
                        onCheckedChange = { viewModel.toggleDualThreshold() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF00325B),
                            checkedTrackColor = if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary,
                            uncheckedThumbColor = Color(0xFF64748B),
                            uncheckedTrackColor = Color(0xFF1E2D48)
                        ),
                        modifier = Modifier.testTag("dual_threshold_switch")
                    )
                }

                if (state.dualThresholdActive) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF1A2A42),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tracking ${state.activeSensorSource.shortLabel}: ${String.format("%.1f°C", state.effectiveControlTemp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )

                            val statusMsg = if (state.power && state.mode == state.dualTargetMode) {
                                "${state.dualTargetMode.shortCode} RUNNING"
                            } else if (state.effectiveControlTemp <= state.dualCutInTemp) {
                                "CUT-IN TRIGGER ACTIVE"
                            } else {
                                "TEMPERATURE SATISFIED"
                            }

                            Text(
                                text = statusMsg,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (statusMsg.contains("RUNNING")) EcoGreen else CyanPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Center Climate Wheel
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            ClimateWheel(
                targetTemp = state.targetTemp,
                ambientTemp = state.effectiveControlTemp,
                isPowerOn = state.power,
                mode = state.mode,
                onTempChange = { viewModel.setTargetTemperature(it) }
            )
        }

        // Environmental Metrics Bar (Tuya vs Blaster Battery)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, SurfaceCardBorderDark),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E2D48), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📡", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Tuya Room Sensor", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            "${String.format("%.1f°C", state.tuyaTemp)} / ${state.tuyaHumidity}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = SurfaceDark,
                border = BorderStroke(1.dp, SurfaceCardBorderDark),
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF1E2D48), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.BatteryStd,
                            contentDescription = null,
                            tint = EcoGreen,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Blaster Batt (${state.blasterBatteryLevel}%)", style = MaterialTheme.typography.labelSmall, color = Color(0xFF94A3B8), fontSize = 10.sp)
                        Text(
                            String.format("%.1f°C", state.blasterBatteryTemp + state.blasterCalibratedOffset),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Mode Selector (Cool, Heat, Dry, Fan, Eco)
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            ModeSelector(
                selectedMode = state.mode,
                isPowerOn = state.power,
                onSelectMode = { viewModel.setMode(it) }
            )
        }

        // Fan Speed Selector
        Box(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
            FanSpeedSelector(
                selectedFanSpeed = state.fanSpeed,
                isPowerOn = state.power,
                onSelectFanSpeed = { viewModel.setFanSpeed(it) }
            )
        }

        // Quick Function Toggles Row
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "UNIT FUNCTIONS",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickToggleItem(
                    title = "Swing",
                    isActive = state.swing,
                    icon = Icons.Default.SwapVert,
                    activeColor = CyanPrimary,
                    enabled = state.power,
                    onClick = { viewModel.toggleSwing() },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_swing"
                )

                QuickToggleItem(
                    title = "Turbo",
                    isActive = state.turbo,
                    icon = Icons.Default.Bolt,
                    activeColor = TurboOrange,
                    enabled = state.power,
                    onClick = { viewModel.toggleTurbo() },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_turbo"
                )

                QuickToggleItem(
                    title = "Sleep",
                    isActive = state.sleep,
                    icon = Icons.Default.Bedtime,
                    activeColor = Color(0xFFA78BFA),
                    enabled = state.power,
                    onClick = { viewModel.toggleSleep() },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_sleep"
                )

                QuickToggleItem(
                    title = "Display",
                    isActive = state.displayLed,
                    icon = Icons.Default.Lightbulb,
                    activeColor = EcoGreen,
                    enabled = state.power,
                    onClick = { viewModel.toggleDisplayLed() },
                    modifier = Modifier.weight(1f),
                    testTag = "toggle_led"
                )
            }
        }

        // Last IR Transmission Status Card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SettingsRemote,
                        contentDescription = "IR Transmitter status",
                        tint = CyanPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "LAST IR BURST: ${state.lastTransmittedCommand}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = state.lastTransmittedHex,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = if (state.mode == ACMode.HEAT) TurboOrange else CyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF1E2D48),
                    modifier = Modifier.clickable { onNavigateToRemote() }
                ) {
                    Text(
                        text = "Remote Pad ›",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }

    if (showCloudBackupDialog) {
        AlertDialog(
            onDismissRequest = { showCloudBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDone, contentDescription = null, tint = CyanPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Packaged APK Export", color = Color.White)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Exports the compiled Android APK binary package ('GLP09PORT_Aircon.apk', ~23 MB) with all IR blaster, Tuya sensor, and dual-threshold heating logic directly to your Google Drive and Gmail.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFCBD5E1)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            viewModel.setUserEmail(it)
                        },
                        label = { Text("Account Email (Gmail)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "• SAVE APK TO DRIVE: Uploads 'GLP09PORT_Aircon.apk' directly to your Google Drive.\n\n• EMAIL APK TO GMAIL: Dispatches 'GLP09PORT_Aircon.apk' package directly to your Gmail inbox.\n\n• DIRECT DOWNLOAD: You can also tap 'Settings' -> 'Generate APK/AAB' in the AI Studio platform menu to download it immediately to your phone.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8),
                        lineHeight = 16.sp
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = {
                            viewModel.shareApkToDriveOrGmail(context)
                            showCloudBackupDialog = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Share APK via Android Apps (Drive / Gmail)")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                viewModel.sendBackupToGmail()
                                showCloudBackupDialog = false
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Email APK", fontSize = 12.sp)
                        }

                        ElevatedButton(
                            onClick = {
                                viewModel.backupToGoogleDrive()
                                showCloudBackupDialog = false
                            },
                            colors = ButtonDefaults.elevatedButtonColors(
                                containerColor = CyanPrimary,
                                contentColor = Color(0xFF00325B)
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("APK to Drive", fontSize = 12.sp)
                        }
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showCloudBackupDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun QuickToggleItem(
    title: String,
    isActive: Boolean,
    icon: ImageVector,
    activeColor: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isActive && enabled) activeColor.copy(alpha = 0.2f) else SurfaceDark,
        border = BorderStroke(1.dp, if (isActive && enabled) activeColor else SurfaceCardBorderDark),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(enabled = enabled) { onClick() }
            .testTag(testTag)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isActive && enabled) activeColor else if (enabled) Color(0xFF94A3B8) else Color(0xFF475569),
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = if (isActive && enabled) activeColor else if (enabled) Color.White else Color(0xFF64748B),
                fontWeight = if (isActive && enabled) FontWeight.Bold else FontWeight.Medium,
                fontSize = 11.sp
            )
        }
    }
}
