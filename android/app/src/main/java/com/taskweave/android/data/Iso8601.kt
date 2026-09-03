package com.taskweave.android.data

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Lenient ISO-8601 parsing for the mix of formats the backend emits. */
object Iso8601 {

    fun parse(value: String?): Instant? {
        val raw = value?.trim().orEmpty()
        if (raw.isEmpty()) return null
        return runCatching { Instant.parse(raw) }
            .recoverCatching { OffsetDateTime.parse(raw).toInstant() }
            .recoverCatching {
                LocalDateTime.parse(raw).atZone(ZoneId.systemDefault()).toInstant()
            }
            .recoverCatching {
                LocalDate.parse(raw).atStartOfDay(ZoneId.systemDefault()).toInstant()
            }
            .getOrNull()
    }

    fun format(instant: Instant?): String? =
        instant?.let { DateTimeFormatter.ISO_INSTANT.format(it) }
}
