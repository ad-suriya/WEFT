package com.taskweave.android

import com.taskweave.android.data.local.entity.PendingOpType
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.remote.dto.TaskDto
import com.taskweave.android.data.repository.TaskRepository
import com.taskweave.android.fakes.FakePendingOpDao
import com.taskweave.android.fakes.FakeTaskDao
import com.taskweave.android.fakes.FakeTaskWeaveApi
import com.taskweave.android.fakes.RecordingWorkScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TaskRepositoryTest {

    private lateinit var api: FakeTaskWeaveApi
    private lateinit var taskDao: FakeTaskDao
    private lateinit var pendingOpDao: FakePendingOpDao
    private lateinit var scheduler: RecordingWorkScheduler
    private lateinit var repo: TaskRepository

    @Before
    fun setUp() {
        api = FakeTaskWeaveApi()
        taskDao = FakeTaskDao()
        pendingOpDao = FakePendingOpDao()
        scheduler = RecordingWorkScheduler()
        repo = TaskRepository(
            api = api,
            taskDao = taskDao,
            pendingOpDao = pendingOpDao,
            json = Json { ignoreUnknownKeys = true },
            syncScheduler = scheduler,
        )
    }

    @Test
    fun `addTask online persists the server row and queues nothing`() = runTest {
        val result = repo.addTask("Write tests")

        val task = result.getOrThrow()
        assertTrue((task.id.toIntOrNull() != null))
        assertFalse(task.pendingSync)
        assertEquals(1, api.server.size)
        assertTrue(pendingOpDao.all().isEmpty())
        assertEquals(listOf("Write tests"), taskDao.snapshot().map { it.title })
    }

    @Test
    fun `addTask offline keeps an optimistic row and queues a CREATE`() = runTest {
        api.online = false

        val result = repo.addTask("Buy milk")

        assertTrue(result.isSuccess)
        val row = taskDao.snapshot().single()
        assertTrue(row.id.startsWith("local-"))
        assertTrue(row.pendingSync)
        assertEquals(PendingOpType.CREATE.name, pendingOpDao.all().single().type)
        assertEquals(1, scheduler.pendingSyncRequests)
        assertEquals(0, api.server.size)
    }

    @Test
    fun `replayPending promotes a queued CREATE to a real server row`() = runTest {
        api.online = false
        repo.addTask("Draft email")
        val localId = taskDao.snapshot().single().id

        api.online = true
        val drained = repo.replayPending()

        assertTrue(drained)
        assertTrue(pendingOpDao.all().isEmpty())
        val row = taskDao.snapshot().single()
        assertTrue((row.id.toIntOrNull() != null))
        assertFalse(row.pendingSync)
        assertNull(taskDao.getById(localId))
        assertEquals(1, api.server.size)
    }

    @Test
    fun `toggleComplete offline is optimistic then reconciles on replay`() = runTest {
        val task = repo.addTask("Ship it").getOrThrow()

        api.online = false
        repo.toggleComplete(task)

        with(taskDao.snapshot().single()) {
            assertEquals(TaskStatus.COMPLETED.wire, status)
            assertTrue(pendingSync)
        }
        assertEquals(PendingOpType.UPDATE.name, pendingOpDao.all().single().type)

        api.online = true
        assertTrue(repo.replayPending())

        assertEquals(TaskStatus.COMPLETED.wire, api.server.values.single().status)
        assertFalse(taskDao.snapshot().single().pendingSync)
    }

    @Test
    fun `deleteTask offline soft-deletes and queues, replay removes the row`() = runTest {
        val task = repo.addTask("Temp").getOrThrow()

        api.online = false
        val result = repo.deleteTask(task.id)

        assertTrue(result.isSuccess)
        assertTrue(taskDao.getById(task.id)!!.deletedLocally)
        assertTrue(repo.tasks.first().none { it.id == task.id }) // hidden from the UI immediately
        assertEquals(PendingOpType.DELETE.name, pendingOpDao.all().single().type)

        api.online = true
        assertTrue(repo.replayPending())

        assertNull(taskDao.getById(task.id))
        assertTrue(pendingOpDao.all().isEmpty())
        assertEquals(0, api.server.size)
    }

    @Test
    fun `refresh replaces server rows but never clobbers an unsynced local row`() = runTest {
        val kept = repo.addTask("Keep").getOrThrow()      // -> server id "1"

        api.online = false
        repo.addTask("Pending")                            // -> local-… queued
        val localId = taskDao.snapshot().first { it.id.startsWith("local-") }.id

        // Something else created a task server-side in the meantime.
        api.server["99"] = TaskDto(id = 99, taskName = "From another device")

        api.online = true
        assertTrue(repo.refresh().isSuccess)

        val ids = taskDao.snapshot().map { it.id }.toSet()
        assertTrue(kept.id in ids)
        assertTrue("99" in ids)
        assertTrue(localId in ids)
        assertTrue(taskDao.getById(localId)!!.pendingSync)
    }
}
