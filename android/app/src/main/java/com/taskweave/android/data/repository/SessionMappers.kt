package com.taskweave.android.data.repository

import com.taskweave.android.data.Iso8601
import com.taskweave.android.data.model.FocusSession
import com.taskweave.android.data.remote.dto.SessionDto
import java.time.Instant

fun SessionDto.toDomain(): FocusSession = FocusSession(
    id = id?.toString() ?: error("session without id from backend"),
    description = description,
    startedAt = Iso8601.parse(startTime) ?: Instant.now(),
    endedAt = Iso8601.parse(endTime),
    plannedMinutes = durationMinutes,
    isPaused = isPaused,
    breaksTaken = breaksTaken,
    totalBreakMinutes = totalBreakMinutes,
)
