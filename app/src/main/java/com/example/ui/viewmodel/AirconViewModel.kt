package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AirconRepository
import com.example.data.db.CommandLogEntity
import com.example.data.db.SensorLogEntity
import com.example.data.model.ACMode
import com.example.data.model.AirconState
import com.example.data.model.AutomationRule
import com.example.data.model.ConditionType
import com.example.data.model.FanSpeed
import com.example.data.model.ScheduleOverride
import com.example.data.model.SensorSource
import com.example.data.model.TransmitterMode
import com.example.ir.Glp09PortIrProtocol
import com.example.ir.IrTransmitterManager
import com.example.sensor.StuckSensorReport
import com.example.sensor.StuckSensorWatchdog
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class AirconViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AirconRepository(application)
    private val irManager = IrTransmitterManager(application)
    private val watchdog = StuckSensorWatchdog()
    private val driveGmailService = com.example.cloud.GoogleDriveGmailService(application)

    // User Google Account Email
    private val _userEmail = MutableStateFlow("hencofouche8@gmail.com")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun setUserEmail(email: String) {
        _userEmail.value = email.trim()
    }

    fun backupToGoogleDrive() {
        if (_isCloudSyncing.value) return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _userMessage.emit("Packaging compiled GLP09PORT_Aircon.apk and uploading to Google Drive...")
            val result = driveGmailService.uploadApkToGoogleDrive()
            _isCloudSyncing.value = false
            _userMessage.emit(result.message)
        }
    }

    fun sendBackupToGmail() {
        if (_isCloudSyncing.value) return
        viewModelScope.launch {
            _isCloudSyncing.value = true
            _userMessage.emit("Attaching GLP09PORT_Aircon.apk and dispatching to ${_userEmail.value} via Gmail...")
            val result = driveGmailService.sendApkToGmail(
                recipientEmail = _userEmail.value,
                state = _state.value
            )
            _isCloudSyncing.value = false
            _userMessage.emit(result.message)
        }
    }

    fun shareApkToDriveOrGmail(context: Context) {
        val success = driveGmailService.shareApkToDriveOrGmail(context)
        if (!success) {
            viewModelScope.launch {
                _userMessage.emit("APK is ready in project build outputs. Use the AI Studio top Settings menu to export the APK file directly.")
            }
        }
    }

    // Main Aircon State
    private val _state = MutableStateFlow(repository.loadSavedState())
    val state: StateFlow<AirconState> = _state.asStateFlow()

    // Diagnostic Stuck Sensor Report
    private val _stuckReport = MutableStateFlow(
        StuckSensorReport(
            isStuck = false,
            stuckMinutes = 0,
            variance = 0.4,
            reason = "Initial sensor monitoring active",
            recommendedAction = "Normal operation"
        )
    )
    val stuckReport: StateFlow<StuckSensorReport> = _stuckReport.asStateFlow()

    // Toast/Snackbar notifications
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // DB flows
    val automationRules: StateFlow<List<AutomationRule>> = repository.automationRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sensorLogs: StateFlow<List<SensorLogEntity>> = repository.recentSensorLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val commandLogs: StateFlow<List<CommandLogEntity>> = repository.recentCommands
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var flushJob: Job? = null
    private var preFlushMode: ACMode = ACMode.COOL
    private var preFlushFan: FanSpeed = FanSpeed.AUTO
    private var lastAutomationTriggerEpochMs: Long = 0

    init {
        // Detect hardware IR transmitter support
        val hasHwIr = irManager.hasHardwareIrEmitter()
        _state.update {
            it.copy(
                hasBuiltInIr = hasHwIr,
                transmitterMode = if (hasHwIr) TransmitterMode.BUILTIN_IR else TransmitterMode.SIMULATION
            )
        }

        // Initialize default rules if database is empty
        viewModelScope.launch {
            repository.initializeDefaultRulesIfNeeded()
        }

        // Background ticker loop (1 sec interval for countdowns, sensor watchdog, automation)
        startBackgroundLoops()
    }

    private fun startBackgroundLoops() {
        viewModelScope.launch {
            var counter = 0
            while (isActive) {
                delay(1000)
                counter++

                val now = System.currentTimeMillis()

                // 1. Tick Schedule Override countdown
                val currentOverride = _state.value.activeOverride
                if (currentOverride != null && !currentOverride.isPermanentHold) {
                    if (currentOverride.isExpired(now)) {
                        _state.update { it.copy(activeOverride = null) }
                        _userMessage.emit("Schedule override '${currentOverride.name}' ended. Restored baseline.")
                    }
                }

                // 2. Simulate small ambient & sensor temperature fluctuations or drift
                if (!_state.value.isFlushingSensor) {
                    driftTemperatures()
                }

                // 3. Every 5 seconds: Evaluate stuck sensor watchdog & dual-threshold / automation rules
                if (counter % 5 == 0) {
                    val s = _state.value
                    watchdog.recordReading(s.ambientTemp, s.power)
                    val report = watchdog.analyze(s, now)
                    _stuckReport.value = report
                    _state.update {
                        it.copy(
                            isSensorStuck = report.isStuck,
                            stuckDurationMinutes = report.stuckMinutes
                        )
                    }

                    // Check automation & dual-threshold hysteresis
                    evaluateAutomationRules(report)
                }

                // 4. Every 20 seconds: Record sensor log to Room database
                if (counter % 20 == 0) {
                    val current = _state.value
                    repository.logSensorReading(
                        temp = current.effectiveControlTemp,
                        target = current.targetTemp,
                        mode = current.mode.shortCode,
                        isStuck = current.isSensorStuck,
                        note = "${current.activeSensorSource.shortLabel} (${if (current.isFlushingSensor) "Purge" else (current.activeOverride?.let { "Override: ${it.name}" } ?: "Nominal")})"
                    )
                }
            }
        }
    }

    private fun driftTemperatures() {
        val s = _state.value
        var newIntakeTemp = s.ambientTemp
        var newTuyaTemp = s.tuyaTemp
        var newBlasterTemp = s.blasterBatteryTemp

        if (s.power) {
            when (s.mode) {
                ACMode.HEAT -> {
                    // In Heat mode, temperature rises toward target (e.g. 25°C)
                    if (newIntakeTemp < s.targetTemp.toDouble()) {
                        newIntakeTemp += 0.03
                    }
                    if (newTuyaTemp < s.targetTemp.toDouble()) {
                        newTuyaTemp += 0.02
                    }
                    if (newBlasterTemp < s.targetTemp.toDouble()) {
                        newBlasterTemp += 0.025
                    }
                }
                ACMode.COOL -> {
                    if (newIntakeTemp > s.targetTemp.toDouble()) {
                        newIntakeTemp -= 0.03
                    } else if (newIntakeTemp < s.targetTemp.toDouble() - 0.5) {
                        newIntakeTemp += 0.02
                    }
                    if (newTuyaTemp > s.targetTemp.toDouble()) {
                        newTuyaTemp -= 0.02
                    }
                    if (newBlasterTemp > s.targetTemp.toDouble()) {
                        newBlasterTemp -= 0.025
                    }
                }
                ACMode.ECO -> {
                    if (newIntakeTemp > (s.targetTemp.toDouble() + 0.5)) {
                        newIntakeTemp -= 0.02
                    } else {
                        newIntakeTemp += 0.015
                    }
                }
                ACMode.DRY -> {
                    newIntakeTemp -= 0.01
                }
                ACMode.FAN_ONLY -> {
                    if (newIntakeTemp < 24.5) newIntakeTemp += 0.02 else newIntakeTemp -= 0.02
                }
            }
        } else {
            // Aircon off, room drifts toward natural ambient (e.g. 23.5°C in cool weather)
            if (newIntakeTemp > 23.5) newIntakeTemp -= 0.02 else newIntakeTemp += 0.02
            if (newTuyaTemp > 23.5) newTuyaTemp -= 0.015 else newTuyaTemp += 0.015
            if (newBlasterTemp > 23.5) newBlasterTemp -= 0.02 else newBlasterTemp += 0.02
        }

        _state.update {
            it.copy(
                ambientTemp = ((newIntakeTemp * 10).toInt()) / 10.0,
                tuyaTemp = ((newTuyaTemp * 10).toInt()) / 10.0,
                blasterBatteryTemp = ((newBlasterTemp * 10).toInt()) / 10.0
            )
        }
    }

    // ==========================================
    // Core Aircon Control Actions (IR Blaster)
    // ==========================================

    fun togglePower() {
        val newPower = !_state.value.power
        _state.update { it.copy(power = newPower) }
        transmitCurrentState(if (newPower) "POWER_ON" else "POWER_OFF")
    }

    fun setTargetTemperature(temp: Int) {
        val clamped = temp.coerceIn(16, 31)
        if (_state.value.targetTemp == clamped) return
        _state.update { it.copy(targetTemp = clamped) }
        transmitCurrentState("SET_TEMP_${clamped}C")
    }

    fun adjustTargetTemperature(delta: Int) {
        setTargetTemperature(_state.value.targetTemp + delta)
    }

    fun setMode(mode: ACMode) {
        if (_state.value.mode == mode) return
        _state.update { it.copy(mode = mode) }
        transmitCurrentState("SET_MODE_${mode.name}")
    }

    fun setFanSpeed(fanSpeed: FanSpeed) {
        if (_state.value.fanSpeed == fanSpeed) return
        _state.update { it.copy(fanSpeed = fanSpeed) }
        transmitCurrentState("SET_FAN_${fanSpeed.name}")
    }

    fun toggleSwing() {
        val newSwing = !_state.value.swing
        _state.update { it.copy(swing = newSwing) }
        transmitCurrentState("TOGGLE_SWING_${if (newSwing) "ON" else "OFF"}")
    }

    fun toggleTurbo() {
        val newTurbo = !_state.value.turbo
        _state.update {
            it.copy(
                turbo = newTurbo,
                fanSpeed = if (newTurbo) FanSpeed.HIGH else it.fanSpeed,
                targetTemp = if (newTurbo) 16 else it.targetTemp
            )
        }
        transmitCurrentState("TOGGLE_TURBO_${if (newTurbo) "ON" else "OFF"}")
    }

    fun toggleSleep() {
        val newSleep = !_state.value.sleep
        _state.update { it.copy(sleep = newSleep) }
        transmitCurrentState("TOGGLE_SLEEP_${if (newSleep) "ON" else "OFF"}")
    }

    fun toggleDisplayLed() {
        val newLed = !_state.value.displayLed
        _state.update { it.copy(displayLed = newLed) }
        transmitCurrentState("TOGGLE_LED_${if (newLed) "ON" else "OFF"}")
    }

    fun setTransmitterMode(mode: TransmitterMode) {
        _state.update { it.copy(transmitterMode = mode) }
        repository.saveState(_state.value)
    }

    fun setWifiGatewayUrl(url: String) {
        _state.update { it.copy(wifiGatewayUrl = url) }
        repository.saveState(_state.value)
    }

    // ==========================================
    // Sensor Source & Tuya / Blaster Battery Integration
    // ==========================================

    fun setActiveSensorSource(source: SensorSource) {
        _state.update { it.copy(activeSensorSource = source) }
        repository.saveState(_state.value)
        viewModelScope.launch {
            _userMessage.emit("Active Sensor Source changed to: ${source.title}")
        }
    }

    fun setTuyaTempManual(temp: Double, humidity: Int = 51) {
        val clamped = ((temp * 10).toInt()) / 10.0
        _state.update {
            it.copy(
                tuyaTemp = clamped,
                tuyaHumidity = humidity,
                tuyaLastSyncEpochMs = System.currentTimeMillis()
            )
        }
        repository.saveState(_state.value)
    }

    fun syncTuyaSensor() {
        viewModelScope.launch {
            _userMessage.emit("Polling Tuya / Smart Life external sensor...")
            delay(600) // Network sync simulation
            _state.update {
                it.copy(
                    tuyaLastSyncEpochMs = System.currentTimeMillis()
                )
            }
            _userMessage.emit("Tuya Sensor synced: ${String.format("%.1f°C", _state.value.tuyaTemp)}, ${_state.value.tuyaHumidity}% RH")
        }
    }

    fun setTuyaEndpoint(url: String) {
        _state.update { it.copy(tuyaEndpointUrl = url) }
        repository.saveState(_state.value)
    }

    fun setBlasterCalibratedOffset(offset: Double) {
        val clamped = ((offset.coerceIn(-5.0, 5.0) * 10).toInt()) / 10.0
        _state.update { it.copy(blasterCalibratedOffset = clamped) }
        repository.saveState(_state.value)
        viewModelScope.launch {
            _userMessage.emit("Blaster battery sensor offset calibrated to ${if (clamped >= 0) "+$clamped" else clamped}°C")
        }
    }

    fun setBlasterTempManual(temp: Double) {
        val clamped = ((temp * 10).toInt()) / 10.0
        _state.update { it.copy(blasterBatteryTemp = clamped) }
        repository.saveState(_state.value)
    }

    // ==========================================
    // Dual-Threshold Hysteresis Automation
    // (User requirement: Put on at 24°C and off at 25°C)
    // ==========================================

    fun toggleDualThreshold() {
        val newActive = !_state.value.dualThresholdActive
        _state.update { it.copy(dualThresholdActive = newActive) }
        repository.saveState(_state.value)
        viewModelScope.launch {
            _userMessage.emit(
                if (newActive)
                    "Dual-Threshold Thermostat ACTIVE: ON at ${_state.value.dualCutInTemp}°C, OFF at ${_state.value.dualCutOutTemp}°C (${_state.value.activeSensorSource.shortLabel})"
                else
                    "Dual-Threshold Thermostat Disabled."
            )
        }
    }

    fun setDualThresholdConfig(
        active: Boolean,
        cutIn: Double,
        cutOut: Double,
        mode: ACMode = ACMode.HEAT
    ) {
        _state.update {
            it.copy(
                dualThresholdActive = active,
                dualCutInTemp = cutIn,
                dualCutOutTemp = cutOut,
                dualTargetMode = mode
            )
        }
        repository.saveState(_state.value)
        viewModelScope.launch {
            _userMessage.emit("Updated Dual-Threshold: ON at ${cutIn}°C, OFF at ${cutOut}°C (${mode.displayName})")
        }
    }

    private fun transmitCurrentState(commandName: String, isFlush: Boolean = false) {
        val currentState = _state.value
        repository.saveState(currentState)

        viewModelScope.launch {
            val result = irManager.transmit(currentState, commandName, isFlush)
            _state.update {
                it.copy(
                    lastTransmittedCommand = commandName,
                    lastTransmittedHex = result.hexCode,
                    lastTransmitSuccess = result.success
                )
            }
            repository.logCommand(
                commandName = commandName,
                hexCode = result.hexCode,
                modeUsed = result.modeUsed.title,
                success = result.success
            )
            if (!result.success) {
                _userMessage.emit("IR Transmission: ${result.message}")
            }
        }
    }

    // ==========================================
    // Stuck Sensor Reset & Diagnostics Workflows
    // ==========================================

    fun startThermalSiphonFlush() {
        if (_state.value.isFlushingSensor) return

        preFlushMode = _state.value.mode
        preFlushFan = _state.value.fanSpeed

        _state.update {
            it.copy(
                isFlushingSensor = true,
                flushSecondsRemaining = 90,
                power = true,
                mode = ACMode.FAN_ONLY,
                fanSpeed = FanSpeed.HIGH
            )
        }

        transmitCurrentState("PURGE_THERMAL_SIPHON_START", isFlush = true)

        viewModelScope.launch {
            _userMessage.emit("Thermal Siphon Flush initiated (90s High Fan Purge)...")
            repository.logSensorReading(
                temp = _state.value.effectiveAmbientTemp,
                target = _state.value.targetTemp,
                mode = "PURGE",
                isStuck = false,
                note = "Initiated 90s Thermal Siphon Flush to clear trapped thermistor air"
            )
        }

        flushJob?.cancel()
        flushJob = viewModelScope.launch {
            for (sec in 90 downTo 1) {
                delay(1000)
                _state.update { it.copy(flushSecondsRemaining = sec - 1) }
            }
            completeThermalFlush()
        }
    }

    fun cancelThermalFlush() {
        flushJob?.cancel()
        completeThermalFlush(isCancelled = true)
    }

    private fun completeThermalFlush(isCancelled: Boolean = false) {
        watchdog.resetAfterFlush()
        _state.update {
            it.copy(
                isFlushingSensor = false,
                flushSecondsRemaining = 0,
                mode = preFlushMode,
                fanSpeed = preFlushFan,
                ambientTemp = 25.2,
                isSensorStuck = false,
                stuckDurationMinutes = 0
            )
        }
        transmitCurrentState("PURGE_THERMAL_SIPHON_END", isFlush = false)

        viewModelScope.launch {
            val msg = if (isCancelled) "Thermal Siphon Flush stopped." else "Thermal Siphon Flush complete! Sensor reading unlatched & recalibrated."
            _userMessage.emit(msg)
            repository.logSensorReading(
                temp = 25.2 + _state.value.sensorOffset,
                target = _state.value.targetTemp,
                mode = _state.value.mode.shortCode,
                isStuck = false,
                note = if (isCancelled) "Flush manually aborted" else "Flush completed successfully; thermistor reset."
            )
        }
    }

    fun executeHardSensorResetSequence() {
        viewModelScope.launch {
            _userMessage.emit("Sending micro-power cycle IR reset pulse...")
            _state.update { it.copy(power = false) }
            val offRes = irManager.transmit(_state.value, "RESET_STEP_POWER_OFF")
            repository.logCommand("RESET_STEP_POWER_OFF", offRes.hexCode, offRes.modeUsed.title, offRes.success)
            delay(2500)

            val fanState = _state.value.copy(power = true, mode = ACMode.FAN_ONLY, fanSpeed = FanSpeed.HIGH)
            val fanRes = irManager.transmit(fanState, "RESET_STEP_HIGH_BLOW")
            repository.logCommand("RESET_STEP_HIGH_BLOW", fanRes.hexCode, fanRes.modeUsed.title, fanRes.success)
            delay(2000)

            _state.update {
                it.copy(
                    power = true,
                    mode = ACMode.COOL,
                    ambientTemp = 24.8,
                    isSensorStuck = false,
                    stuckDurationMinutes = 0
                )
            }
            watchdog.resetAfterFlush()
            transmitCurrentState("RESET_STEP_RE_ARM_COOL")
            _userMessage.emit("Micro-cycle reset complete. GLP-09PORT logic recalibrated.")
        }
    }

    fun setSensorOffset(offset: Double) {
        val clamped = ((offset.coerceIn(-5.0, 5.0) * 10).toInt()) / 10.0
        _state.update { it.copy(sensorOffset = clamped) }
        repository.saveState(_state.value)
        viewModelScope.launch {
            _userMessage.emit("AC internal intake offset calibrated to ${if (clamped >= 0) "+$clamped" else clamped}°C")
        }
    }

    fun calibrateSensorReadingToReference(referenceTemp: Double) {
        val raw = _state.value.ambientTemp
        val newOffset = ((referenceTemp - raw) * 10).toInt() / 10.0
        setSensorOffset(newOffset)
    }

    fun toggleSimulateStuckSensor() {
        val currentlyStuck = _state.value.isSensorStuck
        if (!currentlyStuck) {
            watchdog.setSimulateStuck(true, 18.2)
            _state.update {
                it.copy(
                    ambientTemp = 18.2,
                    isSensorStuck = true,
                    stuckDurationMinutes = 48
                )
            }
            viewModelScope.launch {
                _userMessage.emit("Simulated stuck sensor injected (frozen at 18.2°C).")
            }
        } else {
            watchdog.resetAfterFlush()
            _state.update {
                it.copy(
                    ambientTemp = 25.0,
                    isSensorStuck = false,
                    stuckDurationMinutes = 0
                )
            }
            viewModelScope.launch {
                _userMessage.emit("Simulated stuck condition cleared.")
            }
        }
    }

    // ==========================================
    // Schedule Overrides
    // ==========================================

    fun applyQuickOverride(
        name: String,
        targetTemp: Int,
        mode: ACMode,
        fanSpeed: FanSpeed,
        durationMinutes: Int,
        isPermanentHold: Boolean = false
    ) {
        val override = ScheduleOverride(
            id = UUID.randomUUID().toString(),
            name = name,
            targetTemp = targetTemp,
            mode = mode,
            fanSpeed = fanSpeed,
            startedAtEpochMs = System.currentTimeMillis(),
            durationMinutes = durationMinutes,
            isPermanentHold = isPermanentHold
        )
        _state.update {
            it.copy(
                activeOverride = override,
                power = true,
                targetTemp = targetTemp,
                mode = mode,
                fanSpeed = fanSpeed
            )
        }
        transmitCurrentState("SCHEDULE_OVERRIDE_${override.name.replace(" ", "_").uppercase()}")
        viewModelScope.launch {
            _userMessage.emit("Active Override: '$name' for ${if (isPermanentHold) "Permanent Hold" else "$durationMinutes mins"}")
        }
    }

    fun extendOverride(extraMinutes: Int = 30) {
        val current = _state.value.activeOverride ?: return
        val updated = current.copy(durationMinutes = current.durationMinutes + extraMinutes)
        _state.update { it.copy(activeOverride = updated) }
        viewModelScope.launch {
            _userMessage.emit("Override extended by $extraMinutes min (Total: ${updated.durationMinutes} min)")
        }
    }

    fun cancelOverride() {
        val current = _state.value.activeOverride
        _state.update { it.copy(activeOverride = null) }
        viewModelScope.launch {
            _userMessage.emit("Cancelled override '${current?.name ?: ""}'. Returned to standard schedule.")
        }
    }

    // ==========================================
    // Automation Rules Engine
    // ==========================================

    fun toggleAutomationRule(rule: AutomationRule) {
        viewModelScope.launch {
            val updated = rule.copy(isEnabled = !rule.isEnabled)
            repository.saveAutomationRule(updated)
            _userMessage.emit("Rule '${rule.title}' ${if (updated.isEnabled) "Enabled" else "Disabled"}")
        }
    }

    fun saveOrUpdateRule(rule: AutomationRule) {
        viewModelScope.launch {
            repository.saveAutomationRule(rule)
            _userMessage.emit("Saved rule: '${rule.title}'")
        }
    }

    fun deleteRule(ruleId: String) {
        viewModelScope.launch {
            repository.deleteAutomationRule(ruleId)
            _userMessage.emit("Rule deleted")
        }
    }

    private fun evaluateAutomationRules(report: StuckSensorReport) {
        val now = System.currentTimeMillis()
        if (now - lastAutomationTriggerEpochMs < 20_000L) return

        // Active temporary override has absolute priority
        if (_state.value.activeOverride != null) return

        val s = _state.value
        val measuredControlTemp = s.effectiveControlTemp

        // 1. Evaluate User's Dual-Threshold Hysteresis Automation (ON at 24°C, OFF at 25°C)
        if (s.dualThresholdActive) {
            if (s.dualTargetMode == ACMode.HEAT) {
                // HEATING: When temperature drops to or below cut-in (e.g. 24.0°C), turn ON Heat
                if (measuredControlTemp <= s.dualCutInTemp && (!s.power || s.mode != ACMode.HEAT)) {
                    lastAutomationTriggerEpochMs = now
                    _state.update {
                        it.copy(
                            power = true,
                            mode = ACMode.HEAT,
                            targetTemp = s.dualCutOutTemp.toInt(),
                            fanSpeed = s.dualTargetFanSpeed
                        )
                    }
                    transmitCurrentState("AUTO_HEAT_ON_AT_${s.dualCutInTemp.toInt()}C")
                    viewModelScope.launch {
                        _userMessage.emit("Dual-Threshold: Measured ${String.format("%.1f°C", measuredControlTemp)} <= ${s.dualCutInTemp}°C (${s.activeSensorSource.shortLabel}). Turned ON HEAT!")
                    }
                    return
                }
                // When temperature reaches or exceeds cut-out (e.g. 25.0°C), turn OFF
                else if (measuredControlTemp >= s.dualCutOutTemp && s.power && s.mode == ACMode.HEAT) {
                    lastAutomationTriggerEpochMs = now
                    _state.update { it.copy(power = false) }
                    transmitCurrentState("AUTO_HEAT_OFF_AT_${s.dualCutOutTemp.toInt()}C")
                    viewModelScope.launch {
                        _userMessage.emit("Dual-Threshold: Measured ${String.format("%.1f°C", measuredControlTemp)} >= ${s.dualCutOutTemp}°C (${s.activeSensorSource.shortLabel}). Turned OFF!")
                    }
                    return
                }
            } else if (s.dualTargetMode == ACMode.COOL) {
                // COOLING: When temperature rises to or above cut-out (e.g. 25.0°C), turn ON Cool
                if (measuredControlTemp >= s.dualCutOutTemp && (!s.power || s.mode != ACMode.COOL)) {
                    lastAutomationTriggerEpochMs = now
                    _state.update {
                        it.copy(
                            power = true,
                            mode = ACMode.COOL,
                            targetTemp = s.dualCutInTemp.toInt(),
                            fanSpeed = s.dualTargetFanSpeed
                        )
                    }
                    transmitCurrentState("AUTO_COOL_ON_AT_${s.dualCutOutTemp.toInt()}C")
                    viewModelScope.launch {
                        _userMessage.emit("Dual-Threshold: Measured ${String.format("%.1f°C", measuredControlTemp)} >= ${s.dualCutOutTemp}°C (${s.activeSensorSource.shortLabel}). Turned ON COOL!")
                    }
                    return
                }
                // When cooled down to or below cut-in (e.g. 24.0°C), turn OFF
                else if (measuredControlTemp <= s.dualCutInTemp && s.power && s.mode == ACMode.COOL) {
                    lastAutomationTriggerEpochMs = now
                    _state.update { it.copy(power = false) }
                    transmitCurrentState("AUTO_COOL_OFF_AT_${s.dualCutInTemp.toInt()}C")
                    viewModelScope.launch {
                        _userMessage.emit("Dual-Threshold: Cooled to ${String.format("%.1f°C", measuredControlTemp)} <= ${s.dualCutInTemp}°C. Turned OFF!")
                    }
                    return
                }
            }
        }

        // 2. Evaluate general automation rules
        val enabledRules = automationRules.value.filter { it.isEnabled }
        val cal = Calendar.getInstance()
        val currentHourMin = SimpleDateFormat("HH:mm", Locale.getDefault()).format(cal.time)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val appDay = if (dayOfWeek == Calendar.SUNDAY) 7 else (dayOfWeek - 1)

        for (rule in enabledRules) {
            if (!rule.activeDays.contains(appDay)) continue

            var triggered = false

            when (rule.conditionType) {
                ConditionType.TEMP_ABOVE -> {
                    if (isWithinTimeWindow(currentHourMin, rule.timeStart, rule.timeEnd)) {
                        if (measuredControlTemp >= rule.thresholdValue) {
                            triggered = true
                        }
                    }
                }
                ConditionType.TEMP_BELOW -> {
                    if (isWithinTimeWindow(currentHourMin, rule.timeStart, rule.timeEnd)) {
                        if (measuredControlTemp <= rule.thresholdValue) {
                            triggered = true
                        }
                    }
                }
                ConditionType.HUMIDITY_ABOVE -> {
                    if (s.humidity.toDouble() >= rule.thresholdValue) {
                        triggered = true
                    }
                }
                ConditionType.STUCK_SENSOR_TRIGGER -> {
                    if (report.isStuck && report.stuckMinutes >= rule.thresholdValue) {
                        triggered = true
                    }
                }
                ConditionType.TIME_WINDOW -> {
                    if (isWithinTimeWindow(currentHourMin, rule.timeStart, rule.timeEnd)) {
                        if (!s.power || s.mode != rule.actionMode || s.targetTemp != rule.actionTemp) {
                            triggered = true
                        }
                    }
                }
            }

            if (triggered) {
                lastAutomationTriggerEpochMs = now
                viewModelScope.launch {
                    val updatedRule = rule.copy(lastTriggeredEpochMs = now)
                    repository.saveAutomationRule(updatedRule)

                    if (rule.triggerSensorFlush) {
                        startThermalSiphonFlush()
                        _userMessage.emit("Automation [${rule.title}]: Auto-triggered Thermal Siphon Flush!")
                    } else {
                        _state.update {
                            it.copy(
                                power = rule.actionPower,
                                targetTemp = rule.actionTemp,
                                mode = rule.actionMode,
                                fanSpeed = rule.actionFanSpeed
                            )
                        }
                        transmitCurrentState("AUTO_${rule.title.replace(" ", "_").uppercase()}")
                        _userMessage.emit("Automation triggered: '${rule.title}'")
                    }
                }
                break
            }
        }
    }

    private fun isWithinTimeWindow(current: String, start: String, end: String): Boolean {
        return if (start <= end) {
            current in start..end
        } else {
            current >= start || current <= end
        }
    }
}
