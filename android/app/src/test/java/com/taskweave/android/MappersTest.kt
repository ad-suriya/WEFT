package com.taskweave.android

import com.taskweave.android.data.model.TaskPriority
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.remote.dto.TaskDto
import com.taskweave.android.data.repository.decodeList
import com.taskweave.android.data.repository.encodeList
import com.taskweave.android.data.repository.toDomain
import com.taskweave.android.data.repository.toEntity
import com.taskweave.android.data.repository.toWriteDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MappersTest {

    @Test
    fun `dto maps to domain with parsed times and enums`() {
        val dto = TaskDto(
            id = 7,
            taskName = "Write spec",
            status = "IN_PROGRESS",
            urgency = "HIGH",
            deadline = "2026-09-01T17:00:00Z",
            scheduledStart = "2026-09-01T09:00:00Z",
            nextMicroStep = "outline the intro",
        )

        val task = dto.toDomain()

        assertEquals("7", task.id)
        assertEquals("Write spec", task.title)
        assertEquals(TaskStatus.IN_PROGRESS, task.status)
        assertEquals(TaskPriority.HIGH, task.priority)
        assertEquals(Instant.parse("2026-09-01T17:00:00Z"), task.dueDate)
        assertEquals(Instant.parse("2026-09-01T09:00:00Z"), task.scheduledStart)
        assertEquals("outline the intro", task.nextStep)
    }

    @Test
    fun `unknown status and blank micro-step normalize`() {
        val task = TaskDto(id = 1, taskName = "t", status = "weird", urgency = null, nextMicroStep = "").toDomain()
        assertEquals(TaskStatus.TODO, task.status)
        assertEquals(TaskPriority.NONE, task.priority)
        assertNull(task.nextStep)
    }

    @Test
    fun `COMPLETED status round-trips`() {
        val task = TaskDto(id = 2, taskName = "done one", status = "COMPLETED").toDomain()
        assertEquals(TaskStatus.COMPLETED, task.status)
        assertTrue(task.isDone)
        assertEquals("COMPLETED", task.toWriteDto().status)
    }

    @Test
    fun `domain survives a round-trip through the Room entity`() {
        val original = TaskDto(
            id = 42,
            taskName = "Round trip",
            selectedText = "notes",
            status = "COMPLETED",
            urgency = "LOW",
            scheduledStart = "2026-09-02T08:30:00Z",
            estimatedMinutes = 45,
            tags = listOf("a", "b"),
            url = "https://example.com",
        ).toDomain()

        assertEquals(original, original.toEntity().toDomain())
    }

    @Test
    fun `write dto uses task_name and drops NONE urgency and empty tags`() {
        val body = TaskDto(id = 3, taskName = "T", urgency = "none", tags = emptyList())
            .toDomain()
            .toWriteDto()

        assertEquals("T", body.taskName)
        assertNull(body.urgency)
        assertNull(body.tags)
    }

    @Test
    fun `list json encode-decode is a round-trip and tolerates garbage`() {
        assertEquals(listOf("x", "y"), decodeList(encodeList(listOf("x", "y"))))
        assertTrue(decodeList(null).isEmpty())
        assertTrue(decodeList("not json").isEmpty())
    }
}
