package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "automation_rules")
data class AutomationRuleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val conditionType: String,
    val thresholdValue: Double,
    val timeStart: String,
    val timeEnd: String,
    val activeDaysCsv: String,
    val actionPower: Boolean,
    val actionTemp: Int,
    val actionMode: String,
    val actionFanSpeed: String,
    val triggerSensorFlush: Boolean,
    val isEnabled: Boolean,
    val lastTriggeredEpochMs: Long?
)

@Entity(tableName = "sensor_logs")
data class SensorLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val ambientTemp: Double,
    val targetTemp: Int,
    val mode: String,
    val isStuck: Boolean,
    val note: String
)

@Entity(tableName = "command_logs")
data class CommandLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long,
    val commandName: String,
    val hexCode: String,
    val modeUsed: String,
    val success: Boolean
)
