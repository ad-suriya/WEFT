package com.taskweave.android.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.taskweave.android.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE deletedLocally = 0 ORDER BY scheduledStartEpochMs IS NULL, scheduledStartEpochMs ASC, updatedAtEpochMs DESC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: String): TaskEntity?

    @Upsert
    suspend fun upsert(task: TaskEntity)

    @Upsert
    suspend fun upsertAll(tasks: List<TaskEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE tasks SET deletedLocally = 1, pendingSync = 1 WHERE id = :id")
    suspend fun markDeleted(id: String)

    @Query("SELECT id FROM tasks")
    suspend fun allIds(): List<String>

    @Query("DELETE FROM tasks WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>)

    /** Replace the server-known rows with a fresh snapshot, keeping unsynced local rows. */
    @Transaction
    suspend fun replaceServerSnapshot(fresh: List<TaskEntity>) {
        val freshIds = fresh.map { it.id }.toSet()
        val stale = allIds().filter { it !in freshIds }
        // Only drop rows that have nothing pending; a queued create/delete stays.
        val toDelete = stale.filter { id -> getById(id)?.pendingSync != true }
        if (toDelete.isNotEmpty()) deleteByIds(toDelete)
        fresh.forEach { incoming ->
            val local = getById(incoming.id)
            if (local?.pendingSync == true) return@forEach // local edit wins until replayed
            upsert(incoming)
        }
    }
}
