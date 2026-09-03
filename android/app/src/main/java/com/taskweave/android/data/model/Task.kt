package com.taskweave.android.data.model

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Domain model used by the UI. Mapping to/from [TaskDto] lives in the repository. */
data class Task(
    val id: String,
    val title: String,
    val description: String? = null,
    val status: TaskStatus = TaskStatus.TODO,
    val priority: TaskPriority = TaskPriority.NONE,
    val dueDate: Instant? = null,
    val scheduledStart: Instant? = null,
    val scheduledEnd: Instant? = null,
    val estimatedMinutes: Int? = null,
    val nextStep: String? = null,
    val microSteps: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val project: String? = null,
    val url: String? = null,
    /** From `/api/tasks` `risk.risk_level`: low | medium | high | null. */
    val riskLevel: String? = null,
    /** True while an optimistic local change has not yet been confirmed by the backend. */
    val pendingSync: Boolean = false,
) {
    val isDone: Boolean get() = status == TaskStatus.COMPLETED

    fun isScheduledOn(day: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Boolean =
        scheduledStart?.atZone(zone)?.toLocalDate() == day
}

enum class TaskStatus(val wire: String) {
    TODO("TODO"),
    IN_PROGRESS("IN_PROGRESS"),
    COMPLETED("COMPLETED"),
    SKIPPED("SKIPPED");

    companion object {
        fun fromWire(value: String?): TaskStatus =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: TODO
    }
}

/** The backend calls this "urgency". */
enum class TaskPriority(val wire: String) {
    NONE("NONE"),
    LOW("LOW"),
    MEDIUM("MEDIUM"),
    HIGH("HIGH");

    companion object {
        fun fromWire(value: String?): TaskPriority =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) } ?: NONE
    }
}
