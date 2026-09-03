package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A task document from the Task Weave backend (`/api/tasks`). Field names match
 * the live API (verified against the deployed backend), not the app's domain
 * model — mapping happens in [com.taskweave.android.data.repository.toDomain].
 */
@Serializable
data class TaskDto(
    val id: Int? = null,
    @SerialName("task_name") val taskName: String = "",
    @SerialName("selected_text") val selectedText: String? = null,
    val status: String? = null,                 // TODO | IN_PROGRESS | COMPLETED
    val urgency: String? = null,                // LOW | MEDIUM | HIGH
    val deadline: String? = null,
    @SerialName("scheduled_start") val scheduledStart: String? = null,
    @SerialName("scheduled_end") val scheduledEnd: String? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("completed_minutes") val completedMinutes: Int = 0,
    @SerialName("next_micro_step") val nextMicroStep: String? = null,
    val url: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    val risk: RiskDto? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class RiskDto(
    @SerialName("task_id") val taskId: Int? = null,
    @SerialName("risk_score") val riskScore: Double? = null,
    @SerialName("risk_percent") val riskPercent: Int? = null,
    @SerialName("risk_level") val riskLevel: String? = null,     // low | medium | high
    @SerialName("remaining_hours") val remainingHours: Double? = null,
    @SerialName("usable_hours") val usableHours: Double? = null,
    @SerialName("productivity_factor") val productivityFactor: Double? = null,
    val reason: String? = null,
)

/** Body for `POST /api/tasks` and `PATCH /api/tasks/{id}` (all fields optional on PATCH). */
@Serializable
data class TaskWriteDto(
    @SerialName("task_name") val taskName: String? = null,
    @SerialName("selected_text") val selectedText: String? = null,
    val status: String? = null,
    val urgency: String? = null,
    val deadline: String? = null,
    @SerialName("scheduled_start") val scheduledStart: String? = null,
    @SerialName("scheduled_end") val scheduledEnd: String? = null,
    @SerialName("estimated_minutes") val estimatedMinutes: Int? = null,
    @SerialName("next_micro_step") val nextMicroStep: String? = null,
    val url: String? = null,
    val tags: List<String>? = null,
)
