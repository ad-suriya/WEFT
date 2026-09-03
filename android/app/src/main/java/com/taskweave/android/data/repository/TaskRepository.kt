package com.taskweave.android.data.repository

import com.taskweave.android.data.local.dao.PendingOpDao
import com.taskweave.android.data.local.dao.TaskDao
import com.taskweave.android.data.local.entity.PendingOpEntity
import com.taskweave.android.data.local.entity.PendingOpType
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.TaskWriteDto
import com.taskweave.android.sync.WorkScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException
import java.time.Instant
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for tasks in the UI. Room is the cache the UI observes;
 * every write is applied to Room first (optimistic) and then pushed to the API.
 * A push that fails with a network error is queued in `pending_ops` and replayed
 * by [com.taskweave.android.sync.PendingOpWorker].
 */
@Singleton
class TaskRepository @Inject constructor(
    private val api: TaskWeaveApi,
    private val taskDao: TaskDao,
    private val pendingOpDao: PendingOpDao,
    private val json: Json,
    private val syncScheduler: WorkScheduler,
) {
    val tasks: Flow<List<Task>> = taskDao.observeTasks().map { list -> list.map { it.toDomain() } }

    val pendingWriteCount: Flow<Int> = pendingOpDao.count()

    /** Pull the authoritative list; local unsynced rows are preserved. */
    suspend fun refresh(): Result<Unit> = runCatching {
        val remote = api.getTasks().map { it.toDomain().toEntity() }
        taskDao.replaceServerSnapshot(remote)
    }

    suspend fun addTask(
        title: String,
        description: String? = null,
    ): Result<Task> {
        val tempId = "local-${UUID.randomUUID()}"
        val optimistic = Task(
            id = tempId,
            title = title.trim(),
            description = description?.trim()?.takeIf { it.isNotEmpty() },
            pendingSync = true,
        )
        taskDao.upsert(optimistic.toEntity())

        val body = TaskWriteDto(
            taskName = optimistic.title,
            selectedText = optimistic.description,
        )
        return pushCreate(tempId, body)
    }

    suspend fun updateTask(task: Task): Result<Task> {
        taskDao.upsert(task.copy(pendingSync = true).toEntity())
        val body = task.toWriteDto()
        return pushUpdate(task.id, body)
    }

    suspend fun setStatus(task: Task, status: TaskStatus): Result<Task> =
        updateTask(task.copy(status = status))

    suspend fun toggleComplete(task: Task): Result<Task> =
        setStatus(task, if (task.isDone) TaskStatus.TODO else TaskStatus.COMPLETED)

    /** `POST /api/tasks/{id}/skip` — engine marks it skipped for flexible-streak accounting. */
    suspend fun skip(id: String): Result<Unit> = runCatching {
        api.skipTask(id)
        refresh()
    }

    suspend fun deleteTask(id: String): Result<Unit> {
        taskDao.markDeleted(id)
        if (id.startsWith("local-")) {
            // Never reached the server — just drop it and any queued create.
            pendingOpDao.deleteForTask(id)
            taskDao.hardDelete(id)
            return Result.success(Unit)
        }
        return runCatching { api.deleteTask(id) }
            .fold(
                onSuccess = { taskDao.hardDelete(id); Result.success(Unit) },
                onFailure = { e -> queueOrFail(PendingOpType.DELETE, id, "", e).map { } },
            )
    }

    // --- push helpers --------------------------------------------------------
    private suspend fun pushCreate(tempId: String, body: TaskWriteDto): Result<Task> =
        runCatching { api.createTask(body).toDomain() }
            .fold(
                onSuccess = { created ->
                    taskDao.hardDelete(tempId)
                    pendingOpDao.deleteForTask(tempId)
                    taskDao.upsert(created.toEntity())
                    Result.success(created)
                },
                onFailure = { e ->
                    queueOrFail(PendingOpType.CREATE, tempId, json.encodeToString(body), e)
                        .map { Task(id = tempId, title = body.taskName.orEmpty(), pendingSync = true) }
                },
            )

    private suspend fun pushUpdate(id: String, body: TaskWriteDto): Result<Task> {
        if (id.startsWith("local-")) {
            // The create hasn't landed yet; fold the edit into the queued create.
            pendingOpDao.deleteForTask(id)
            pendingOpDao.enqueue(newOp(PendingOpType.CREATE, id, json.encodeToString(body)))
            syncScheduler.requestPendingSync()
            return Result.success(Task(id = id, title = body.taskName.orEmpty(), pendingSync = true))
        }
        return runCatching { api.updateTask(id, body).toDomain() }
            .fold(
                onSuccess = { updated -> taskDao.upsert(updated.toEntity()); Result.success(updated) },
                onFailure = { e ->
                    queueOrFail(PendingOpType.UPDATE, id, json.encodeToString(body), e)
                        .map { Task(id = id, title = body.taskName.orEmpty(), pendingSync = true) }
                },
            )
    }

    private suspend fun queueOrFail(
        type: PendingOpType,
        taskId: String,
        payload: String,
        error: Throwable,
    ): Result<Unit> {
        if (error !is IOException) return Result.failure(error) // real API error — surface it
        pendingOpDao.enqueue(newOp(type, taskId, payload))
        syncScheduler.requestPendingSync()
        return Result.success(Unit)
    }

    private companion object {
        const val MAX_REPLAY_ATTEMPTS = 6
    }

    private fun newOp(type: PendingOpType, taskId: String, payload: String) = PendingOpEntity(
        type = type.name,
        taskId = taskId,
        payloadJson = payload,
        createdAtEpochMs = Instant.now().toEpochMilli(),
    )

    // --- replay (called by PendingOpWorker) --------------------------------
    suspend fun replayPending(): Boolean {
        var allDone = true
        for (op in pendingOpDao.all()) {
            val result = runCatching { applyOp(op) }
            when {
                result.getOrDefault(false) -> pendingOpDao.delete(op)
                // Drop a poison op (server keeps rejecting it) rather than loop forever.
                op.attempts >= MAX_REPLAY_ATTEMPTS && result.exceptionOrNull() !is IOException -> {
                    pendingOpDao.delete(op)
                }
                else -> {
                    pendingOpDao.bumpAttempts(op.id)
                    allDone = false
                }
            }
        }
        return allDone
    }

    private suspend fun applyOp(op: PendingOpEntity): Boolean = when (PendingOpType.valueOf(op.type)) {
        PendingOpType.CREATE -> {
            val body = json.decodeFromString<TaskWriteDto>(op.payloadJson)
            val created = api.createTask(body).toDomain()
            taskDao.hardDelete(op.taskId)
            taskDao.upsert(created.toEntity())
            true
        }
        PendingOpType.UPDATE -> {
            val body = json.decodeFromString<TaskWriteDto>(op.payloadJson)
            val updated = api.updateTask(op.taskId, body).toDomain()
            taskDao.upsert(updated.toEntity())
            true
        }
        PendingOpType.DELETE -> {
            api.deleteTask(op.taskId)
            taskDao.hardDelete(op.taskId)
            true
        }
        PendingOpType.SKIP -> {
            api.skipTask(op.taskId)
            true
        }
    }
}
