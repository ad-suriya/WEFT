package com.taskweave.android.data.model

/**
 * The behavioural mode the engine infers from your state. The backend has no
 * "get mode" endpoint — it's returned by `/api/chat` as `current_mode`; the app
 * caches the last one and falls back to a status-derived guess.
 */
enum class Mode(val wire: String, val label: String) {
    PLANNING("PLANNING_MODE", "Planning"),
    FOCUS("FOCUS_MODE", "Focus"),
    PANIC("PANIC_MODE", "Panic"),
    REVIEW("REVIEW_MODE", "Review");

    companion object {
        fun fromWire(value: String?): Mode? =
            entries.firstOrNull { it.wire.equals(value, ignoreCase = true) }
    }
}
