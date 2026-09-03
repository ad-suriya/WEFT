package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `GET /api/reminders` item (verified against the live API). */
@Serializable
data class ReminderDto(
    val id: Int? = null,
    val message: String = "",
    @SerialName("remind_at") val remindAt: String? = null,
    val kind: String? = null,                     // FOCUS_START | DEADLINE | CUSTOM | ...
    @SerialName("task_id") val taskId: Int? = null,
    /** Backend returns 0/1, not a bool. */
    val acknowledged: Int = 0,
    val due: Boolean = false,
    @SerialName("pushed_at") val pushedAt: String? = null,
)

@Serializable
data class ReminderWriteDto(
    val message: String,
    @SerialName("remind_at") val remindAt: String,
    val kind: String = "CUSTOM",
    @SerialName("task_id") val taskId: Int? = null,
)

/** `POST /api/devices` — register this device's FCM token for push. */
@Serializable
data class DeviceRegisterDto(
    val token: String,
    val platform: String = "android",
)
