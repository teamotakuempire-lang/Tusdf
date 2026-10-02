package com.example.data.model

data class ScheduleOverride(
    val id: String,
    val name: String,
    val targetTemp: Int,
    val mode: ACMode,
    val fanSpeed: FanSpeed,
    val startedAtEpochMs: Long,
    val durationMinutes: Int,
    val isPermanentHold: Boolean = false
) {
    val expiresAtEpochMs: Long
        get() = if (isPermanentHold) Long.MAX_VALUE else startedAtEpochMs + (durationMinutes * 60 * 1000L)

    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean {
        if (isPermanentHold) return false
        return nowMs >= expiresAtEpochMs
    }

    fun remainingSeconds(nowMs: Long = System.currentTimeMillis()): Long {
        if (isPermanentHold) return -1L
        val diff = (expiresAtEpochMs - nowMs) / 1000L
        return if (diff > 0) diff else 0L
    }

    fun formatRemainingTime(nowMs: Long = System.currentTimeMillis()): String {
        if (isPermanentHold) return "Permanent Hold"
        val remainingSec = remainingSeconds(nowMs)
        val hrs = remainingSec / 3600
        val mins = (remainingSec % 3600) / 60
        val secs = remainingSec % 60
        return if (hrs > 0) {
            String.format("%dh %02dm", hrs, mins)
        } else {
            String.format("%02dm %02ds", mins, secs)
        }
    }
}
