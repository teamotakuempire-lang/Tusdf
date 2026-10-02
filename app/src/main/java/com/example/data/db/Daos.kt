package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AutomationRuleDao {
    @Query("SELECT * FROM automation_rules ORDER BY title ASC")
    fun getAllRules(): Flow<List<AutomationRuleEntity>>

    @Query("SELECT * FROM automation_rules WHERE isEnabled = 1")
    suspend fun getEnabledRules(): List<AutomationRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(rule: AutomationRuleEntity)

    @Update
    suspend fun update(rule: AutomationRuleEntity)

    @Delete
    suspend fun delete(rule: AutomationRuleEntity)

    @Query("DELETE FROM automation_rules WHERE id = :id")
    suspend fun deleteById(id: String)
}

@Dao
interface SensorLogDao {
    @Query("SELECT * FROM sensor_logs ORDER BY timestampMs DESC LIMIT 60")
    fun getRecentLogs(): Flow<List<SensorLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SensorLogEntity)

    @Query("DELETE FROM sensor_logs WHERE timestampMs < :cutoffMs")
    suspend fun deleteOldLogs(cutoffMs: Long)

    @Query("DELETE FROM sensor_logs")
    suspend fun clearAll()
}

@Dao
interface CommandLogDao {
    @Query("SELECT * FROM command_logs ORDER BY timestampMs DESC LIMIT 40")
    fun getRecentCommands(): Flow<List<CommandLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: CommandLogEntity)

    @Query("DELETE FROM command_logs")
    suspend fun clearAll()
}
