package com.taskweave.android.data.repository

import com.taskweave.android.data.Iso8601
import com.taskweave.android.data.local.entity.TaskEntity
import com.taskweave.android.data.model.Task
import com.taskweave.android.data.model.TaskPriority
import com.taskweave.android.data.model.TaskStatus
import com.taskweave.android.data.remote.dto.TaskDto
import com.taskweave.android.data.remote.dto.TaskWriteDto
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.time.Instant

private val listJson = Json { ignoreUnknownKeys = true }
private val stringListSerializer = ListSerializer(String.serializer())

fun encodeList(values: List<String>): String = listJson.encodeToString(stringListSerializer, values)
fun decodeList(raw: String?): List<String> =
    if (raw.isNullOrBlank()) emptyList()
    else runCatching { listJson.decodeFromString(stringListSerializer, raw) }.getOrDefault(emptyList())

// --- DTO -> domain -------------------------------------------------------------
fun TaskDto.toDomain(): Task = Task(
    id = id?.toString() ?: error("task without id from backend"),
    title = taskName,
    description = selectedText,
    status = TaskStatus.fromWire(status),
    priority = TaskPriority.fromWire(urgency),
    dueDate = Iso8601.parse(deadline),
    scheduledStart = Iso8601.parse(scheduledStart),
    scheduledEnd = Iso8601.parse(scheduledEnd),
    estimatedMinutes = estimatedMinutes,
    nextStep = nextMicroStep?.takeIf { it.isNotBlank() },
    microSteps = emptyList(),
    tags = tags,
    project = null,
    url = url,
    riskLevel = risk?.riskLevel,
    pendingSync = false,
)

// --- domain <-> entity -------------------------------------------------------
fun Task.toEntity(updatedAt: Instant = Instant.now()): TaskEntity = TaskEntity(
    id = id,
    title = title,
    description = description,
    status = status.wire,
    priority = priority.wire,
    dueDateEpochMs = dueDate?.toEpochMilli(),
    scheduledStartEpochMs = scheduledStart?.toEpochMilli(),
    scheduledEndEpochMs = scheduledEnd?.toEpochMilli(),
    estimatedMinutes = estimatedMinutes,
    nextStep = nextStep,
    microStepsJson = encodeList(microSteps),
    tagsJson = encodeList(tags),
    project = project,
    url = url,
    riskLevel = riskLevel,
    updatedAtEpochMs = updatedAt.toEpochMilli(),
    pendingSync = pendingSync,
    deletedLocally = false,
)

fun TaskEntity.toDomain(): Task = Task(
    id = id,
    title = title,
    description = description,
    status = TaskStatus.fromWire(status),
    priority = TaskPriority.fromWire(priority),
    dueDate = dueDateEpochMs?.let(Instant::ofEpochMilli),
    scheduledStart = scheduledStartEpochMs?.let(Instant::ofEpochMilli),
    scheduledEnd = scheduledEndEpochMs?.let(Instant::ofEpochMilli),
    estimatedMinutes = estimatedMinutes,
    nextStep = nextStep,
    microSteps = decodeList(microStepsJson),
    tags = decodeList(tagsJson),
    project = project,
    url = url,
    riskLevel = riskLevel,
    pendingSync = pendingSync,
)

// --- domain -> write body ---------------------------------------------------
fun Task.toWriteDto(): TaskWriteDto = TaskWriteDto(
    taskName = title,
    selectedText = description,
    status = status.wire,
    urgency = priority.takeIf { it != TaskPriority.NONE }?.wire,
    deadline = Iso8601.format(dueDate),
    scheduledStart = Iso8601.format(scheduledStart),
    scheduledEnd = Iso8601.format(scheduledEnd),
    estimatedMinutes = estimatedMinutes,
    nextMicroStep = nextStep,
    url = url,
    tags = tags.takeIf { it.isNotEmpty() },
)
