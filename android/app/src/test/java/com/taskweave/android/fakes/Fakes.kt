package com.taskweave.android.fakes

import com.taskweave.android.data.local.dao.PendingOpDao
import com.taskweave.android.data.local.dao.TaskDao
import com.taskweave.android.data.local.entity.PendingOpEntity
import com.taskweave.android.data.local.entity.TaskEntity
import com.taskweave.android.data.remote.TaskWeaveApi
import com.taskweave.android.data.remote.dto.ChatRequest
import com.taskweave.android.data.remote.dto.ChatResponse
import com.taskweave.android.data.remote.dto.DeviceRegisterDto
import com.taskweave.android.data.remote.dto.ReminderDto
import com.taskweave.android.data.remote.dto.ReminderWriteDto
import com.taskweave.android.data.remote.dto.ScheduleRequest
import com.taskweave.android.data.remote.dto.ScheduleResponse
import com.taskweave.android.data.remote.dto.StatusResponse
import com.taskweave.android.data.remote.dto.TaskDto
import com.taskweave.android.data.remote.dto.TaskWriteDto
import com.taskweave.android.sync.WorkScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class RecordingWorkScheduler : WorkScheduler {
    var pendingSyncRequests = 0
        private set

    override fun ensurePeriodicStatusSync() = Unit
    override fun syncNow() = Unit
    override fun requestPendingSync() {
        pendingSyncRequests++
    }
}

class FakeTaskDao : TaskDao {
    private val rows = MutableStateFlow<Map<String, TaskEntity>>(emptyMap())

    fun snapshot(): List<TaskEntity> = rows.value.values.toList()

    override fun observeTasks(): Flow<List<TaskEntity>> =
        rows.map { m -> m.values.filterNot { it.deletedLocally }.sortedByDescending { it.updatedAtEpochMs } }

    override suspend fun getById(id: String): TaskEntity? = rows.value[id]

    override suspend fun upsert(task: TaskEntity) {
        rows.value = rows.value + (task.id to task)
    }

    override suspend fun upsertAll(tasks: List<TaskEntity>) {
        rows.value = rows.value + tasks.associateBy { it.id }
    }

    override suspend fun insert(task: TaskEntity) = upsert(task)

    override suspend fun hardDelete(id: String) {
        rows.value = rows.value - id
    }

    override suspend fun markDeleted(id: String) {
        rows.value[id]?.let { rows.value = rows.value + (id to it.copy(deletedLocally = true, pendingSync = true)) }
    }

    override suspend fun allIds(): List<String> = rows.value.keys.toList()

    override suspend fun deleteByIds(ids: List<String>) {
        rows.value = rows.value - ids.toSet()
    }
}

class FakePendingOpDao : PendingOpDao {
    private val seq = AtomicLong(0)
    private val ops = MutableStateFlow<List<PendingOpEntity>>(emptyList())

    override suspend fun enqueue(op: PendingOpEntity): Long {
        val id = seq.incrementAndGet()
        ops.value = ops.value + op.copy(id = id)
        return id
    }

    override suspend fun all(): List<PendingOpEntity> = ops.value.sortedBy { it.id }

    override fun count(): Flow<Int> = ops.map { it.size }

    override suspend fun delete(op: PendingOpEntity) {
        ops.value = ops.value.filterNot { it.id == op.id }
    }

    override suspend fun bumpAttempts(id: Long) {
        ops.value = ops.value.map { if (it.id == id) it.copy(attempts = it.attempts + 1) else it }
    }

    override suspend fun deleteForTask(taskId: String) {
        ops.value = ops.value.filterNot { it.taskId == taskId }
    }
}

/**
 * In-memory [TaskWeaveApi] using the real API's field names. Flip [online] to
 * false to make every task call fail with [IOException] (the signal
 * [com.taskweave.android.data.repository.TaskRepository] uses to queue a write).
 */
class FakeTaskWeaveApi : TaskWeaveApi {
    var online = true
    private val seq = AtomicInteger(0)
    val server = linkedMapOf<String, TaskDto>()

    private fun requireOnline() {
        if (!online) throw IOException("offline")
    }

    override suspend fun getTasks(): List<TaskDto> {
        requireOnline()
        return server.values.toList()
    }

    override suspend fun createTask(body: TaskWriteDto): TaskDto {
        requireOnline()
        val id = seq.incrementAndGet()
        val dto = TaskDto(
            id = id,
            taskName = body.taskName.orEmpty(),
            selectedText = body.selectedText,
            status = body.status ?: "TODO",
            urgency = body.urgency,
        )
        server[id.toString()] = dto
        return dto
    }

    override suspend fun updateTask(id: String, body: TaskWriteDto): TaskDto {
        requireOnline()
        val existing = server[id] ?: TaskDto(id = id.toIntOrNull(), taskName = body.taskName.orEmpty())
        val updated = existing.copy(
            taskName = body.taskName ?: existing.taskName,
            selectedText = body.selectedText ?: existing.selectedText,
            status = body.status ?: existing.status,
            urgency = body.urgency ?: existing.urgency,
        )
        server[id] = updated
        return updated
    }

    override suspend fun deleteTask(id: String) {
        requireOnline()
        server.remove(id)
    }

    override suspend fun skipTask(id: String) {
        requireOnline()
    }

    // --- unused by these tests -------------------------------------------------
    override suspend fun chat(body: ChatRequest): ChatResponse = notNeeded()
    override suspend fun schedule(body: ScheduleRequest): ScheduleResponse = notNeeded()
    override suspend fun reschedule(body: ScheduleRequest): ScheduleResponse = notNeeded()
    override suspend fun status(): StatusResponse = notNeeded()
    override suspend fun getReminders(): List<ReminderDto> = notNeeded()
    override suspend fun createReminder(body: ReminderWriteDto): ReminderDto = notNeeded()
    override suspend fun ackReminder(id: String) = notNeeded()
    override suspend fun deleteReminder(id: String) = notNeeded()
    override suspend fun recoverTasks(body: com.taskweave.android.data.remote.dto.RecoverRequest) = notNeeded()
    override suspend fun getSessions() = notNeeded()
    override suspend fun createSession(body: com.taskweave.android.data.remote.dto.SessionCreateDto) = notNeeded()
    override suspend fun patchSession(id: String, body: com.taskweave.android.data.remote.dto.SessionPatchDto) = notNeeded()
    override suspend fun deleteSession(id: String) = notNeeded()
    override suspend fun registerDevice(body: DeviceRegisterDto) = notNeeded()
    override suspend fun unregisterDevice(token: String) = notNeeded()

    private fun notNeeded(): Nothing = throw UnsupportedOperationException("not used in this test")
}
