package com.taskweave.android.data.model

import java.time.Duration
import java.time.Instant

data class FocusSession(
    val id: String,
    val description: String?,
    val startedAt: Instant,
    val endedAt: Instant?,
    val plannedMinutes: Int,
    val isPaused: Boolean,
    val breaksTaken: Int,
    val totalBreakMinutes: Int,
    /** Local-only: when the current pause began (backend only records breaks on resume). */
    val pausedSince: Instant? = null,
) {
    val isActive: Boolean get() = endedAt == null

    /** Seconds left on the pomodoro, discounting time spent (or currently) on breaks. */
    fun remainingSeconds(now: Instant = Instant.now()): Long {
        if (!isActive || plannedMinutes <= 0) return 0
        val currentBreak = pausedSince?.let { Duration.between(it, now).seconds.coerceAtLeast(0) } ?: 0
        val elapsed = Duration.between(startedAt, now).seconds - totalBreakMinutes * 60L - currentBreak
        return (plannedMinutes * 60L - elapsed).coerceAtLeast(0)
    }

    fun isOvertime(now: Instant = Instant.now()): Boolean =
        isActive && plannedMinutes > 0 && !isPaused && remainingSeconds(now) == 0L
}
