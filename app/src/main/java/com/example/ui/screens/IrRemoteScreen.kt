package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SettingsInputAntenna
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ACMode
import com.example.data.model.FanSpeed
import com.example.data.model.TransmitterMode
import com.example.ir.Glp09PortIrProtocol
import com.example.ui.theme.AlertRed
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
fun IrRemoteScreen(
    viewModel: AirconViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val commandLogs by viewModel.commandLogs.collectAsState()
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var showGatewayDialog by remember { mutableStateOf(false) }
    var gatewayUrlInput by remember(state.wifiGatewayUrl) { mutableStateOf(state.wifiGatewayUrl) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .verticalScroll(scrollState)
            .padding(bottom = 96.dp)
    ) {
        // Title Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "GLP-09PORT IR Blaster",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Direct 38 kHz NEC Remote Protocol",
                    style = MaterialTheme.typography.bodySmall,
                    color = CyanPrimary
                )
            }

            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFF1E2D48),
                border = BorderStroke(1.dp, Color(0xFF2E4166))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SettingsInputAntenna,
                        contentDescription = null,
                        tint = if (state.hasBuiltInIr) EcoGreen else Color(0xFF94A3B8),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "38 kHz",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Transmitter Mode Selector Tabs
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = SurfaceDark,
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "TRANSMISSION HARDWARE CHANNEL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp, start = 4.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TransmitterMode.values().forEach { mode ->
                        val isSelected = state.transmitterMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) CyanPrimary else Color(0xFF1E2D48))
                                .clickable { viewModel.setTransmitterMode(mode) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("trans_mode_${mode.name.lowercase()}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (mode) {
                                    TransmitterMode.BUILTIN_IR -> "Phone IR"
                                    TransmitterMode.WIFI_GATEWAY -> "Wi-Fi Hub"
                                    TransmitterMode.SIMULATION -> "Virtual"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF00325B) else Color(0xFFCBD5E1),
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // If Wi-Fi gateway selected, show URL editor
                if (state.transmitterMode == TransmitterMode.WIFI_GATEWAY) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = gatewayUrlInput,
                            onValueChange = {
                                gatewayUrlInput = it
                                viewModel.setWifiGatewayUrl(it)
                            },
                            label = { Text("Gateway URL (Tasmota / ESP32)", fontSize = 10.sp) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(color = Color.White),
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF0B1320),
                                unfocusedContainerColor = Color(0xFF0B1320),
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )
                    }
                }
            }
        }

        // ==========================================
        // Physical Remote Body (Skeuomorphic Remote Pad)
        // ==========================================
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = Color(0xFF151F30),
            border = BorderStroke(2.dp, Color(0xFF283A57)),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Remote Top IR Emitter Bezel
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 8.dp)
                        .background(Color(0xFF2D0A0A), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF7F1D1D), RoundedCornerShape(4.dp))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Inverted Digital LCD Display on Remote
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF08121E),
                    border = BorderStroke(1.5.dp, Color(0xFF1E3554)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                    ) {
                        // LCD top row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "LOGIC GLP-09PORT",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (state.power) CyanPrimary else Color(0xFF334155),
                                fontWeight = FontWeight.Bold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (state.swing) {
                                    Text(
                                        text = "SWING",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = EcoGreen,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }
                                if (state.turbo) {
                                    Text(
                                        text = "TURBO",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = TurboOrange,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        // LCD Main Temp + Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (state.power) state.mode.shortCode else "OFF",
                                    fontSize = 18.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (state.power) state.mode.accentColor else Color(0xFF475569),
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = if (state.power) "FAN: ${state.fanSpeed.displayName.uppercase()}" else "",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF94A3B8)
                                )
                            }

                            Row(verticalAlignment = Alignment.Top) {
                                Text(
                                    text = if (state.power) "${state.targetTemp}" else "--",
                                    fontSize = 38.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    color = if (state.power) Color(0xFFE2E8F0) else Color(0xFF334155)
                                )
                                Text(
                                    text = "°C",
                                    fontSize = 14.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = if (state.power) CyanPrimary else Color(0xFF334155),
                                    modifier = Modifier.padding(top = 4.dp, start = 2.dp)
                                )
                            }
                        }

                        // LCD Bottom status
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "ROOM: ${String.format("%.1f°C", state.effectiveAmbientTemp)}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B)
                            )
                            if (state.isSensorStuck) {
                                Text(
                                    text = "! SENSOR STUCK",
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = AlertRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Remote Keypad Buttons Grid
                // Row 1: Power (Red), Swing, Turbo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteRoundButton(
                        label = "POWER",
                        icon = Icons.Default.PowerSettingsNew,
                        color = AlertRed,
                        onClick = { viewModel.togglePower() },
                        testTag = "remote_btn_power"
                    )

                    RemoteRoundButton(
                        label = "SWING",
                        icon = Icons.Default.SwapVert,
                        color = if (state.swing) CyanPrimary else Color(0xFF64748B),
                        onClick = { viewModel.toggleSwing() },
                        testTag = "remote_btn_swing"
                    )

                    RemoteRoundButton(
                        label = "TURBO",
                        icon = Icons.Default.Bolt,
                        color = if (state.turbo) TurboOrange else Color(0xFF64748B),
                        onClick = { viewModel.toggleTurbo() },
                        testTag = "remote_btn_turbo"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 2: Temp Up, Mode, Temp Down
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RemoteRoundButton(
                        label = "TEMP -",
                        icon = Icons.Default.ExpandMore,
                        color = CyanPrimary,
                        onClick = { viewModel.adjustTargetTemperature(-1) },
                        testTag = "remote_btn_temp_down"
                    )

                    // Big Mode Switcher Button
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF1E2D48),
                        border = BorderStroke(2.dp, CyanPrimary),
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .clickable {
                                val nextMode = when (state.mode) {
                                    ACMode.COOL -> ACMode.HEAT
                                    ACMode.HEAT -> ACMode.DRY
                                    ACMode.DRY -> ACMode.FAN_ONLY
                                    ACMode.FAN_ONLY -> ACMode.ECO
                                    ACMode.ECO -> ACMode.COOL
                                }
                                viewModel.setMode(nextMode)
                            }
                            .testTag("remote_btn_mode")
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(state.mode.iconEmoji, fontSize = 18.sp)
                            Text("MODE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }

                    RemoteRoundButton(
                        label = "TEMP +",
                        icon = Icons.Default.ExpandLess,
                        color = CyanPrimary,
                        onClick = { viewModel.adjustTargetTemperature(1) },
                        testTag = "remote_btn_temp_up"
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Row 3: Fan Speed, Sleep, Display LED
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    RemoteRoundButton(
                        label = "FAN",
                        icon = Icons.Default.Air,
                        color = CyanPrimary,
                        onClick = {
                            val nextFan = when (state.fanSpeed) {
                                FanSpeed.AUTO -> FanSpeed.LOW
                                FanSpeed.LOW -> FanSpeed.MEDIUM
                                FanSpeed.MEDIUM -> FanSpeed.HIGH
                                FanSpeed.HIGH -> FanSpeed.AUTO
                            }
                            viewModel.setFanSpeed(nextFan)
                        },
                        testTag = "remote_btn_fan"
                    )

                    RemoteRoundButton(
                        label = "SLEEP",
                        icon = Icons.Default.Bedtime,
                        color = if (state.sleep) Color(0xFFA78BFA) else Color(0xFF64748B),
                        onClick = { viewModel.toggleSleep() },
                        testTag = "remote_btn_sleep"
                    )

                    RemoteRoundButton(
                        label = "LED",
                        icon = Icons.Default.Lightbulb,
                        color = if (state.displayLed) EcoGreen else Color(0xFF64748B),
                        onClick = { viewModel.toggleDisplayLed() },
                        testTag = "remote_btn_led"
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Special GLP-09PORT Logic Button: "FLUSH SENSOR" (Dedicated Hardware Unstick Key)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF0C243B),
                    border = BorderStroke(1.5.dp, CyanPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { viewModel.startThermalSiphonFlush() }
                        .testTag("remote_btn_flush_sensor")
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp, horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RestartAlt,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "PURGE / UNSTICK SENSOR",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = CyanPrimary,
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = "Sends high-fan air siphon reset burst",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // Raw IR Signal Packet Inspector
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, SurfaceCardBorderDark),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RAW IR PROTOCOL INSPECTOR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2D48),
                        modifier = Modifier.clickable {
                            val payload = Glp09PortIrProtocol.toWifiGatewayPayload(state)
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("IR Payload", payload))
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = CyanPrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy JSON", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Hex Code Display
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0B1320),
                    border = BorderStroke(1.dp, Color(0xFF1E2D48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "NEC / AIRCON 40-BIT FRAME:",
                            fontSize = 9.sp,
                            color = Color(0xFF64748B),
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = Glp09PortIrProtocol.toHexString(Glp09PortIrProtocol.buildPacket(state)),
                            fontSize = 15.sp,
                            color = CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Wi-Fi JSON gateway payload
                Text(
                    text = Glp09PortIrProtocol.toWifiGatewayPayload(state),
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Recent IR Transmissions Log
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            Text(
                text = "RECENT IR TRANSMISSIONS (${commandLogs.size})",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF94A3B8),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp)
            )

            if (commandLogs.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, SurfaceCardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No IR bursts sent in this session yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                commandLogs.take(5).forEach { cmd ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SurfaceDark,
                        border = BorderStroke(1.dp, SurfaceCardBorderDark),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = cmd.commandName,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = cmd.hexCode,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyanPrimary,
                                    fontSize = 11.sp
                                )
                            }
                            Text(
                                text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(cmd.timestampMs)),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RemoteRoundButton(
    label: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Surface(
            shape = CircleShape,
            color = Color(0xFF1E2D48),
            border = BorderStroke(1.5.dp, color.copy(alpha = 0.6f)),
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .clickable { onClick() }
                .testTag(testTag)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFCBD5E1)
        )
    }
}
