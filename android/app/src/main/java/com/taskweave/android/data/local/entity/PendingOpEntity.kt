package com.taskweave.android.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A write that could not reach the backend (offline / error). [PendingOpWorker]
 * replays these in FIFO order on reconnect.
 */
@Entity(tableName = "pending_ops")
data class PendingOpEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String,                 // CREATE | UPDATE | DELETE | SKIP
    /** Local task id the op applies to (equals the remote id for server-known tasks). */
    val taskId: String,
    /** JSON body for CREATE/UPDATE; empty for DELETE/SKIP. */
    val payloadJson: String,
    val createdAtEpochMs: Long,
    val attempts: Int = 0,
)

enum class PendingOpType { CREATE, UPDATE, DELETE, SKIP }
