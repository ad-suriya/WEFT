package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Body for `POST /api/schedule` / `POST /api/reschedule`. Empty = "schedule my day". */
@Serializable
data class ScheduleRequest(
    val date: String? = null,                    // ISO date; null = today
    @SerialName("task_ids") val taskIds: List<String>? = null,
    val note: String? = null,
)

@Serializable
data class ScheduleResponse(
    val tasks: List<TaskDto> = emptyList(),
    val message: String? = null,
)
