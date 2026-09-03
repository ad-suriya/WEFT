package com.taskweave.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val status: String,
    val priority: String,
    val dueDateEpochMs: Long?,
    val scheduledStartEpochMs: Long?,
    val scheduledEndEpochMs: Long?,
    val estimatedMinutes: Int?,
    val nextStep: String?,
    /** JSON-encoded `List<String>` (Room stores primitives only here). */
    val microStepsJson: String,
    val tagsJson: String,
    val project: String?,
    val url: String?,
    val riskLevel: String?,
    val updatedAtEpochMs: Long,
    val pendingSync: Boolean = false,
    /** Soft-delete marker so an offline delete survives a refresh until replayed. */
    val deletedLocally: Boolean = false,
)
