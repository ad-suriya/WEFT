package com.taskweave.android.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.ui.graphics.vector.ImageVector

enum class TopDestination(
    val route: String,
    val labelRes: Int,
    val icon: ImageVector,
) {
    TODAY("today", com.taskweave.android.R.string.nav_today, Icons.Filled.CalendarToday),
    TASKS("tasks", com.taskweave.android.R.string.nav_tasks, Icons.Filled.Checklist),
    FOCUS("focus", com.taskweave.android.R.string.nav_focus, Icons.Filled.Bolt),
    CHAT("chat", com.taskweave.android.R.string.nav_chat, Icons.AutoMirrored.Filled.Chat);

    companion object {
        const val START = "today"
        fun fromRoute(route: String?): TopDestination? = entries.firstOrNull { it.route == route }
    }
}

object Routes {
    const val SIGN_IN = "sign_in"
    const val HOME = "home"          // hosts the bottom-bar destinations
    fun task(id: String) = "task/$id"
    const val TASK_PATTERN = "task/{taskId}"
    const val BLOCKLIST = "blocklist"
}
