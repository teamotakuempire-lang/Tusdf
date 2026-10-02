package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.data.db.AppDatabase
import com.example.data.db.AutomationRuleEntity
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AirconRepository(context: Context) {
    private val db = AppDatabase.getInstance(context)
    private val ruleDao = db.automationRuleDao()
    private val sensorDao = db.sensorLogDao()
    private val cmdDao = db.commandLogDao()

    private val prefs: SharedPreferences =
        context.getSharedPreferences("glp_aircon_prefs", Context.MODE_PRIVATE)

    val automationRules: Flow<List<AutomationRule>> = ruleDao.getAllRules().map { entities ->
        entities.map { it.toDomain() }
    }

    val recentSensorLogs: Flow<List<SensorLogEntity>> = sensorDao.getRecentLogs()
    val recentCommands: Flow<List<CommandLogEntity>> = cmdDao.getRecentCommands()

    suspend fun saveAutomationRule(rule: AutomationRule) = withContext(Dispatchers.IO) {
        ruleDao.insertOrUpdate(rule.toEntity())
    }

    suspend fun deleteAutomationRule(ruleId: String) = withContext(Dispatchers.IO) {
        ruleDao.deleteById(ruleId)
    }

    suspend fun logSensorReading(temp: Double, target: Int, mode: String, isStuck: Boolean, note: String) = withContext(Dispatchers.IO) {
        sensorDao.insert(
            SensorLogEntity(
                timestampMs = System.currentTimeMillis(),
                ambientTemp = temp,
                targetTemp = target,
                mode = mode,
                isStuck = isStuck,
                note = note
            )
        )
    }

    suspend fun logCommand(commandName: String, hexCode: String, modeUsed: String, success: Boolean) = withContext(Dispatchers.IO) {
        cmdDao.insert(
            CommandLogEntity(
                timestampMs = System.currentTimeMillis(),
                commandName = commandName,
                hexCode = hexCode,
                modeUsed = modeUsed,
                success = success
            )
        )
    }

    suspend fun initializeDefaultRulesIfNeeded() = withContext(Dispatchers.IO) {
        val existing = ruleDao.getEnabledRules()
        if (existing.isEmpty()) {
            val defaultRules = listOf(
                // User explicit requirement: "to put on at 24 c and of at 25 c"
                AutomationRule(
                    id = "rule_dual_heat_24_25",
                    title = "Smart Thermostat (24°C ON / 25°C OFF)",
                    description = "Heating Mode: Turn ON when room drops to 24°C, Turn OFF when room reaches 25°C",
                    conditionType = ConditionType.TEMP_BELOW,
                    thresholdValue = 24.0,
                    timeStart = "00:00",
                    timeEnd = "23:59",
                    actionPower = true,
                    actionTemp = 25,
                    actionMode = ACMode.HEAT,
                    actionFanSpeed = FanSpeed.AUTO,
                    isEnabled = true
                ),
                AutomationRule(
                    id = "rule_heatwave_cool",
                    title = "Hot Afternoon Chill",
                    description = "Activate Cool 22°C when room hits 26.5°C during daytime",
                    conditionType = ConditionType.TEMP_ABOVE,
                    thresholdValue = 26.5,
                    timeStart = "10:00",
                    timeEnd = "19:00",
                    actionPower = true,
                    actionTemp = 22,
                    actionMode = ACMode.COOL,
                    actionFanSpeed = FanSpeed.AUTO,
                    isEnabled = false
                ),
                AutomationRule(
                    id = "rule_stuck_sensor_purge",
                    title = "Stuck Sensor Auto-Purge",
                    description = "Execute 90s Thermal Siphon Flush if intake sensor is stuck >40m",
                    conditionType = ConditionType.STUCK_SENSOR_TRIGGER,
                    thresholdValue = 40.0,
                    actionPower = true,
                    actionTemp = 22,
                    actionMode = ACMode.COOL,
                    actionFanSpeed = FanSpeed.HIGH,
                    triggerSensorFlush = true,
                    isEnabled = true
                )
            )
            for (rule in defaultRules) {
                ruleDao.insertOrUpdate(rule.toEntity())
            }
        }
    }

    fun loadSavedState(): AirconState {
        val power = prefs.getBoolean("power", true)
        val modeOrdinal = prefs.getInt("mode", ACMode.COOL.ordinal)
        val mode = ACMode.values().getOrElse(modeOrdinal) { ACMode.COOL }
        val targetTemp = prefs.getInt("targetTemp", 24)
        val ambientTemp = prefs.getFloat("ambientTemp", 25.4f).toDouble()
        val humidity = prefs.getInt("humidity", 54)
        val fanOrdinal = prefs.getInt("fanSpeed", FanSpeed.AUTO.ordinal)
        val fanSpeed = FanSpeed.values().getOrElse(fanOrdinal) { FanSpeed.AUTO }
        val swing = prefs.getBoolean("swing", false)
        val turbo = prefs.getBoolean("turbo", false)
        val sleep = prefs.getBoolean("sleep", false)
        val displayLed = prefs.getBoolean("displayLed", true)
        val sensorOffset = prefs.getFloat("sensorOffset", 0.0f).toDouble()
        val transModeOrdinal = prefs.getInt("transMode", TransmitterMode.BUILTIN_IR.ordinal)
        val transMode = TransmitterMode.values().getOrElse(transModeOrdinal) { TransmitterMode.BUILTIN_IR }
        val gatewayUrl = prefs.getString("gatewayUrl", "http://192.168.1.120/cm?cmnd=") ?: "http://192.168.1.120/cm?cmnd="

        val sourceOrdinal = prefs.getInt("sensorSource", SensorSource.TUYA_SENSOR.ordinal)
        val sensorSource = SensorSource.values().getOrElse(sourceOrdinal) { SensorSource.TUYA_SENSOR }

        val tuyaTemp = prefs.getFloat("tuyaTemp", 23.8f).toDouble()
        val tuyaHumidity = prefs.getInt("tuyaHumidity", 51)
        val tuyaDeviceName = prefs.getString("tuyaDeviceName", "Tuya Smart Room Sensor") ?: "Tuya Smart Room Sensor"
        val tuyaEndpoint = prefs.getString("tuyaEndpoint", "http://192.168.1.150/tuya/sensor") ?: "http://192.168.1.150/tuya/sensor"

        val blasterBattLevel = prefs.getInt("blasterBattLevel", 89)
        val blasterBattTemp = prefs.getFloat("blasterBattTemp", 24.2f).toDouble()
        val blasterOffset = prefs.getFloat("blasterOffset", -0.4f).toDouble()

        val dualActive = prefs.getBoolean("dualActive", true)
        val dualCutIn = prefs.getFloat("dualCutIn", 24.0f).toDouble()
        val dualCutOut = prefs.getFloat("dualCutOut", 25.0f).toDouble()
        val dualModeOrdinal = prefs.getInt("dualMode", ACMode.HEAT.ordinal)
        val dualMode = ACMode.values().getOrElse(dualModeOrdinal) { ACMode.HEAT }

        return AirconState(
            power = power,
            mode = mode,
            targetTemp = targetTemp,
            ambientTemp = ambientTemp,
            humidity = humidity,
            fanSpeed = fanSpeed,
            swing = swing,
            turbo = turbo,
            sleep = sleep,
            displayLed = displayLed,
            sensorOffset = sensorOffset,
            transmitterMode = transMode,
            wifiGatewayUrl = gatewayUrl,
            activeSensorSource = sensorSource,
            tuyaTemp = tuyaTemp,
            tuyaHumidity = tuyaHumidity,
            tuyaDeviceName = tuyaDeviceName,
            tuyaEndpointUrl = tuyaEndpoint,
            blasterBatteryLevel = blasterBattLevel,
            blasterBatteryTemp = blasterBattTemp,
            blasterCalibratedOffset = blasterOffset,
            dualThresholdActive = dualActive,
            dualCutInTemp = dualCutIn,
            dualCutOutTemp = dualCutOut,
            dualTargetMode = dualMode
        )
    }

    fun saveState(state: AirconState) {
        prefs.edit()
            .putBoolean("power", state.power)
            .putInt("mode", state.mode.ordinal)
            .putInt("targetTemp", state.targetTemp)
            .putFloat("ambientTemp", state.ambientTemp.toFloat())
            .putInt("humidity", state.humidity)
            .putInt("fanSpeed", state.fanSpeed.ordinal)
            .putBoolean("swing", state.swing)
            .putBoolean("turbo", state.turbo)
            .putBoolean("sleep", state.sleep)
            .putBoolean("displayLed", state.displayLed)
            .putFloat("sensorOffset", state.sensorOffset.toFloat())
            .putInt("transMode", state.transmitterMode.ordinal)
            .putString("gatewayUrl", state.wifiGatewayUrl)
            .putInt("sensorSource", state.activeSensorSource.ordinal)
            .putFloat("tuyaTemp", state.tuyaTemp.toFloat())
            .putInt("tuyaHumidity", state.tuyaHumidity)
            .putString("tuyaDeviceName", state.tuyaDeviceName)
            .putString("tuyaEndpoint", state.tuyaEndpointUrl)
            .putInt("blasterBattLevel", state.blasterBatteryLevel)
            .putFloat("blasterBattTemp", state.blasterBatteryTemp.toFloat())
            .putFloat("blasterOffset", state.blasterCalibratedOffset.toFloat())
            .putBoolean("dualActive", state.dualThresholdActive)
            .putFloat("dualCutIn", state.dualCutInTemp.toFloat())
            .putFloat("dualCutOut", state.dualCutOutTemp.toFloat())
            .putInt("dualMode", state.dualTargetMode.ordinal)
            .apply()
    }

    private fun AutomationRuleEntity.toDomain(): AutomationRule {
        val daysList = activeDaysCsv.split(",")
            .mapNotNull { it.trim().toIntOrNull() }
            .ifEmpty { listOf(1, 2, 3, 4, 5, 6, 7) }
        val cond = try { ConditionType.valueOf(conditionType) } catch (e: Exception) { ConditionType.TEMP_ABOVE }
        val m = try { ACMode.valueOf(actionMode) } catch (e: Exception) { ACMode.COOL }
        val f = try { FanSpeed.valueOf(actionFanSpeed) } catch (e: Exception) { FanSpeed.AUTO }
        return AutomationRule(
            id = id,
            title = title,
            description = description,
            conditionType = cond,
            thresholdValue = thresholdValue,
            timeStart = timeStart,
            timeEnd = timeEnd,
            activeDays = daysList,
            actionPower = actionPower,
            actionTemp = actionTemp,
            actionMode = m,
            actionFanSpeed = f,
            triggerSensorFlush = triggerSensorFlush,
            isEnabled = isEnabled,
            lastTriggeredEpochMs = lastTriggeredEpochMs
        )
    }

    private fun AutomationRule.toEntity(): AutomationRuleEntity {
        return AutomationRuleEntity(
            id = id,
            title = title,
            description = description,
            conditionType = conditionType.name,
            thresholdValue = thresholdValue,
            timeStart = timeStart,
            timeEnd = timeEnd,
            activeDaysCsv = activeDays.joinToString(","),
            actionPower = actionPower,
            actionTemp = actionTemp,
            actionMode = actionMode.name,
            actionFanSpeed = actionFanSpeed.name,
            triggerSensorFlush = triggerSensorFlush,
            isEnabled = isEnabled,
            lastTriggeredEpochMs = lastTriggeredEpochMs
        )
    }
}
