package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AvTimer
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreTime
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ACMode
import com.example.data.model.AutomationRule
import com.example.data.model.ConditionType
import com.example.data.model.FanSpeed
import com.example.ui.theme.AlertRed
import com.example.ui.theme.BackgroundDark
import com.example.ui.theme.CyanPrimary
import com.example.ui.theme.EcoGreen
import com.example.ui.theme.SurfaceCardBorderDark
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TurboOrange
import com.example.ui.viewmodel.AirconViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AutomationScheduleScreen(
    viewModel: AirconViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val rules by viewModel.automationRules.collectAsState()
    val scrollState = rememberScrollState()

    var showCreateRuleDialog by remember { mutableStateOf(false) }

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
                text = "Automation & Overrides",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Schedule overrides, thermal trigger rules & auto-purge routines",
                style = MaterialTheme.typography.bodySmall,
                color = CyanPrimary
            )
        }

        // ==========================================
        // 1. Schedule Overrides Control Center
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
                            imageVector = Icons.Default.AvTimer,
                            contentDescription = null,
                            tint = CyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SCHEDULE OVERRIDE CENTER",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF94A3B8),
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    if (state.activeOverride != null) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF1E3554)
                        ) {
                            Text(
                                text = state.activeOverride?.formatRemainingTime() ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = EcoGreen,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Active Override Details or Placeholder
                if (state.activeOverride != null) {
                    val ov = state.activeOverride!!
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF132238),
                        border = BorderStroke(1.dp, Color(0xFF23426D)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(ov.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text(
                                        "Target: ${ov.targetTemp}°C • ${ov.mode.displayName} • ${ov.fanSpeed.displayName} Fan",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = CyanPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelOverride() },
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF475569)),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCBD5E1)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("cancel_override_tab_btn")
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel Override", fontSize = 11.sp)
                                }

                                FilledTonalButton(
                                    onClick = { viewModel.extendOverride(30) },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = ButtonDefaults.filledTonalButtonColors(
                                        containerColor = Color(0xFF1E3554),
                                        contentColor = CyanPrimary
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("extend_override_tab_btn")
                                ) {
                                    Icon(Icons.Default.MoreTime, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+30 Mins", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No active override. The GLP-09PORT is currently adhering to your normal automation schedule rules.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "QUICK OVERRIDE PRESETS (1-TAP):",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp,
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                // Quick Override Buttons Row
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverridePresetChip(
                            title = "1h Boost Chill",
                            desc = "18°C Cool High",
                            onClick = {
                                viewModel.applyQuickOverride(
                                    name = "1h Boost Chill",
                                    targetTemp = 18,
                                    mode = ACMode.COOL,
                                    fanSpeed = FanSpeed.HIGH,
                                    durationMinutes = 60
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "override_preset_1h_boost"
                        )

                        OverridePresetChip(
                            title = "2h Work Focus",
                            desc = "22°C Cool Auto",
                            onClick = {
                                viewModel.applyQuickOverride(
                                    name = "2h Work Focus",
                                    targetTemp = 22,
                                    mode = ACMode.COOL,
                                    fanSpeed = FanSpeed.AUTO,
                                    durationMinutes = 120
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "override_preset_2h_focus"
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OverridePresetChip(
                            title = "Overnight Sleep",
                            desc = "24°C Eco Low (8h)",
                            onClick = {
                                viewModel.applyQuickOverride(
                                    name = "Overnight Sleep",
                                    targetTemp = 24,
                                    mode = ACMode.ECO,
                                    fanSpeed = FanSpeed.LOW,
                                    durationMinutes = 480
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "override_preset_sleep"
                        )

                        OverridePresetChip(
                            title = "Vacation Hold",
                            desc = "Permanent Off",
                            onClick = {
                                viewModel.applyQuickOverride(
                                    name = "Vacation Away Hold",
                                    targetTemp = 26,
                                    mode = ACMode.ECO,
                                    fanSpeed = FanSpeed.LOW,
                                    durationMinutes = 999999,
                                    isPermanentHold = true
                                )
                            },
                            modifier = Modifier.weight(1f),
                            testTag = "override_preset_vacation"
                        )
                    }
                }
            }
        }

        // ==========================================
        // 2. Dual-Threshold Hysteresis Thermostat (24°C ON / 25°C OFF)
        // ==========================================
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, if (state.dualThresholdActive) (if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary) else SurfaceCardBorderDark),
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
                        Text(
                            text = if (state.dualTargetMode == ACMode.HEAT) "☀️" else "❄️",
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "DUAL-THRESHOLD HYSTERESIS",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Automated ON at ${state.dualCutInTemp.toInt()}°C, OFF at ${state.dualCutOutTemp.toInt()}°C",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary
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
                        modifier = Modifier.testTag("dual_threshold_tab_switch")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Reference Sensor: ${state.activeSensorSource.title} (Currently measuring ${String.format("%.1f°C", state.effectiveControlTemp)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFCBD5E1)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Cut-In and Cut-Out adjustment row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Cut-In Box (Turn ON)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF132238),
                        border = BorderStroke(1.dp, Color(0xFF1E3554)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TURN ON (CUT-IN)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text(
                                text = "${state.dualCutInTemp.toInt()}°C",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = if (state.dualTargetMode == ACMode.HEAT) TurboOrange else CyanPrimary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.setDualThresholdConfig(
                                            active = state.dualThresholdActive,
                                            cutIn = (state.dualCutInTemp - 1).coerceAtLeast(16.0),
                                            cutOut = state.dualCutOutTemp,
                                            mode = state.dualTargetMode
                                        )
                                    }
                                ) { Text("-1°C", fontSize = 11.sp) }

                                TextButton(
                                    onClick = {
                                        viewModel.setDualThresholdConfig(
                                            active = state.dualThresholdActive,
                                            cutIn = (state.dualCutInTemp + 1).coerceAtMost(state.dualCutOutTemp - 1),
                                            cutOut = state.dualCutOutTemp,
                                            mode = state.dualTargetMode
                                        )
                                    }
                                ) { Text("+1°C", fontSize = 11.sp) }
                            }
                        }
                    }

                    // Cut-Out Box (Turn OFF)
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF132238),
                        border = BorderStroke(1.dp, Color(0xFF1E3554)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("TURN OFF (CUT-OUT)", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                            Text(
                                text = "${state.dualCutOutTemp.toInt()}°C",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = EcoGreen
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                TextButton(
                                    onClick = {
                                        viewModel.setDualThresholdConfig(
                                            active = state.dualThresholdActive,
                                            cutIn = state.dualCutInTemp,
                                            cutOut = (state.dualCutOutTemp - 1).coerceAtLeast(state.dualCutInTemp + 1),
                                            mode = state.dualTargetMode
                                        )
                                    }
                                ) { Text("-1°C", fontSize = 11.sp) }

                                TextButton(
                                    onClick = {
                                        viewModel.setDualThresholdConfig(
                                            active = state.dualThresholdActive,
                                            cutIn = state.dualCutInTemp,
                                            cutOut = (state.dualCutOutTemp + 1).coerceAtMost(31.0),
                                            mode = state.dualTargetMode
                                        )
                                    }
                                ) { Text("+1°C", fontSize = 11.sp) }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Mode Selector for Dual-Threshold (HEAT vs COOL)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val isHeat = state.dualTargetMode == ACMode.HEAT
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isHeat) TurboOrange.copy(alpha = 0.25f) else Color(0xFF1E2D48),
                        border = BorderStroke(1.dp, if (isHeat) TurboOrange else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setDualThresholdConfig(
                                    active = state.dualThresholdActive,
                                    cutIn = state.dualCutInTemp,
                                    cutOut = state.dualCutOutTemp,
                                    mode = ACMode.HEAT
                                )
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("☀️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Heating Mode",
                                fontSize = 12.sp,
                                fontWeight = if (isHeat) FontWeight.Bold else FontWeight.Normal,
                                color = if (isHeat) TurboOrange else Color(0xFFCBD5E1)
                            )
                        }
                    }

                    val isCool = state.dualTargetMode == ACMode.COOL
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCool) CyanPrimary.copy(alpha = 0.25f) else Color(0xFF1E2D48),
                        border = BorderStroke(1.dp, if (isCool) CyanPrimary else Color.Transparent),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                viewModel.setDualThresholdConfig(
                                    active = state.dualThresholdActive,
                                    cutIn = state.dualCutInTemp,
                                    cutOut = state.dualCutOutTemp,
                                    mode = ACMode.COOL
                                )
                            }
                            .padding(vertical = 8.dp),
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("❄️", fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Cooling Mode",
                                fontSize = 12.sp,
                                fontWeight = if (isCool) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCool) CyanPrimary else Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }
        }

        // ==========================================
        // 3. Smart Automation Rules
        // ==========================================
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "SMART AUTOMATION RULES",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${rules.count { it.isEnabled }} active of ${rules.size} rules",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White
                    )
                }

                ElevatedButton(
                    onClick = { showCreateRuleDialog = true },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = CyanPrimary,
                        contentColor = Color(0xFF00325B)
                    ),
                    modifier = Modifier.testTag("add_rule_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (rules.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SurfaceDark,
                    border = BorderStroke(1.dp, SurfaceCardBorderDark),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No automation rules configured. Tap '+ Add Rule' above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                rules.forEach { rule ->
                    AutomationRuleCard(
                        rule = rule,
                        onToggle = { viewModel.toggleAutomationRule(rule) },
                        onDelete = { viewModel.deleteRule(rule.id) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    // Dialog for creating a new Automation Rule
    if (showCreateRuleDialog) {
        CreateRuleDialog(
            onDismiss = { showCreateRuleDialog = false },
            onSave = { newRule ->
                viewModel.saveOrUpdateRule(newRule)
                showCreateRuleDialog = false
            }
        )
    }
}

@Composable
fun OverridePresetChip(
    title: String,
    desc: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = ""
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFF1E2D48),
        border = BorderStroke(1.dp, Color(0xFF2E4166)),
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .testTag(testTag)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
            Text(desc, style = MaterialTheme.typography.bodySmall, color = CyanPrimary, fontSize = 11.sp)
        }
    }
}

@Composable
fun AutomationRuleCard(
    rule: AutomationRule,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SurfaceDark,
        border = BorderStroke(1.dp, if (rule.isEnabled) SurfaceCardBorderDark else Color(0xFF1A2638)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (rule.isEnabled) Color.White else Color(0xFF64748B)
                    )
                    Text(
                        text = rule.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF94A3B8)
                    )
                }

                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = { onToggle() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color(0xFF00325B),
                        checkedTrackColor = CyanPrimary,
                        uncheckedThumbColor = Color(0xFF64748B),
                        uncheckedTrackColor = Color(0xFF1E2D48)
                    ),
                    modifier = Modifier.testTag("rule_toggle_${rule.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Badge & Condition tag
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color(0xFF1E2D48)
                    ) {
                        Text(
                            text = if (rule.triggerSensorFlush) "PURGE SENSOR" else "${rule.actionTemp}°C ${rule.actionMode.shortCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (rule.triggerSensorFlush) TurboOrange else CyanPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    Text(
                        text = "${rule.timeStart} - ${rule.timeEnd}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF64748B)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete rule",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateRuleDialog(
    onDismiss: () -> Unit,
    onSave: (AutomationRule) -> Unit
) {
    var title by remember { mutableStateOf("Cooling Threshold") }
    var conditionType by remember { mutableStateOf(ConditionType.TEMP_ABOVE) }
    var thresholdStr by remember { mutableStateOf("26.0") }
    var actionTemp by remember { mutableStateOf("22") }
    var selectedMode by remember { mutableStateOf(ACMode.COOL) }
    var selectedFan by remember { mutableStateOf(FanSpeed.AUTO) }
    var triggerFlush by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Automation Rule", color = Color.White) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rule Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("TRIGGER CONDITION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                ConditionType.values().forEach { cond ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                conditionType = cond
                                triggerFlush = cond == ConditionType.STUCK_SENSOR_TRIGGER
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (conditionType == cond) CyanPrimary else Color(0xFF1E2D48),
                            modifier = Modifier.size(16.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(cond.title, color = Color.White, fontSize = 13.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = thresholdStr,
                    onValueChange = { thresholdStr = it },
                    label = { Text("Threshold Value (${conditionType.unit})") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text("ACTION WHEN TRIGGERED:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ACMode.values().forEach { mode ->
                        val isSel = selectedMode == mode
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSel) mode.accentColor.copy(alpha = 0.25f) else Color(0xFF1E2D48),
                            border = BorderStroke(1.dp, if (isSel) mode.accentColor else Color.Transparent),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedMode = mode }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = "${mode.iconEmoji} ${mode.shortCode}",
                                fontSize = 10.sp,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.padding(horizontal = 2.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = actionTemp,
                        onValueChange = { actionTemp = it },
                        label = { Text("Target °C") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { triggerFlush = !triggerFlush }
                ) {
                    Switch(
                        checked = triggerFlush,
                        onCheckedChange = { triggerFlush = it }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Trigger 90s Thermal Siphon Flush", color = Color.White, fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            ElevatedButton(
                onClick = {
                    val threshold = thresholdStr.toDoubleOrNull() ?: 25.0
                    val temp = actionTemp.toIntOrNull() ?: 22
                    val rule = AutomationRule(
                        id = "rule_" + UUID.randomUUID().toString().take(8),
                        title = title,
                        description = "When ${conditionType.title} $threshold${conditionType.unit} -> ${if (triggerFlush) "Purge Sensor" else "Set $temp°C ${selectedMode.shortCode}"}",
                        conditionType = conditionType,
                        thresholdValue = threshold,
                        timeStart = "08:00",
                        timeEnd = "23:00",
                        actionPower = true,
                        actionTemp = temp,
                        actionMode = selectedMode,
                        actionFanSpeed = selectedFan,
                        triggerSensorFlush = triggerFlush,
                        isEnabled = true
                    )
                    onSave(rule)
                },
                colors = ButtonDefaults.elevatedButtonColors(
                    containerColor = CyanPrimary,
                    contentColor = Color(0xFF00325B)
                )
            ) {
                Text("Save Rule")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
