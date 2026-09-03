package com.taskweave.android.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.taskweave.android.data.local.entity.PendingOpEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingOpDao {

    @Insert
    suspend fun enqueue(op: PendingOpEntity): Long

    @Query("SELECT * FROM pending_ops ORDER BY id ASC")
    suspend fun all(): List<PendingOpEntity>

    @Query("SELECT COUNT(*) FROM pending_ops")
    fun count(): Flow<Int>

    @Delete
    suspend fun delete(op: PendingOpEntity)

    @Query("UPDATE pending_ops SET attempts = attempts + 1 WHERE id = :id")
    suspend fun bumpAttempts(id: Long)

    @Query("DELETE FROM pending_ops WHERE taskId = :taskId")
    suspend fun deleteForTask(taskId: String)
}
