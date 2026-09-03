package com.taskweave.android

import com.taskweave.android.data.Iso8601
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class Iso8601Test {

    @Test
    fun `parses a full instant`() {
        assertEquals(
            Instant.parse("2026-08-31T09:30:00Z"),
            Iso8601.parse("2026-08-31T09:30:00Z"),
        )
    }

    @Test
    fun `parses an offset date-time`() {
        assertEquals(
            Instant.parse("2026-08-31T09:30:00Z"),
            Iso8601.parse("2026-08-31T15:00:00+05:30"),
        )
    }

    @Test
    fun `blank and garbage return null`() {
        assertNull(Iso8601.parse(null))
        assertNull(Iso8601.parse(""))
        assertNull(Iso8601.parse("not-a-date"))
    }

    @Test
    fun `round-trips through format`() {
        val now = Instant.parse("2026-01-02T03:04:05Z")
        assertEquals(now, Iso8601.parse(Iso8601.format(now)))
    }
}
