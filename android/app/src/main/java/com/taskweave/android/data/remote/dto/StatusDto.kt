package com.taskweave.android.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * `GET /api/status` (verified against the live API). Task references are ids
 * (ints); per-task detail is in [risks]. [recovery] shape is backend-defined and
 * currently null in practice, so it's kept opaque.
 */
@Serializable
data class StatusResponse(
    val now: String? = null,
    val overdue: List<Int> = emptyList(),
    val slipped: List<Int> = emptyList(),
    @SerialName("at_risk") val atRisk: List<Int> = emptyList(),
    @SerialName("recommend_reschedule") val recommendReschedule: Boolean = false,
    val risks: List<RiskDto> = emptyList(),
    val recovery: JsonElement? = null,
)
