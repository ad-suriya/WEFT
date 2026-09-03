package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A focus/pomodoro session (`/api/sessions`). Active = [endTime] is null. */
@Serializable
data class SessionDto(
    val id: Int? = null,
    val description: String? = null,
    @SerialName("project_id") val projectId: Int? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("is_paused") val isPaused: Boolean = false,
    @SerialName("breaks_taken") val breaksTaken: Int = 0,
    @SerialName("total_break_minutes") val totalBreakMinutes: Int = 0,
    @SerialName("calendar_event_id") val calendarEventId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
)

@Serializable
data class SessionCreateDto(
    val description: String? = null,
    @SerialName("project_id") val projectId: Int? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
)

@Serializable
data class SessionPatchDto(
    val description: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int? = null,
    @SerialName("is_paused") val isPaused: Boolean? = null,
    @SerialName("breaks_taken") val breaksTaken: Int? = null,
    @SerialName("total_break_minutes") val totalBreakMinutes: Int? = null,
)

/** `POST /api/tasks/recover`. Empty list = "let the backend pick". */
@Serializable
data class RecoverRequest(
    @SerialName("task_ids") val taskIds: List<Int> = emptyList(),
)
