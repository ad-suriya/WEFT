package com.taskweave.android.ui.theme

import androidx.compose.ui.graphics.Color
import com.taskweave.android.data.model.Mode
import com.taskweave.android.data.model.TaskPriority

/** Editorial / neo-brutalist palette, shared with the web dashboard. */
object Brand {
    val Cream = Color(0xFFF5F2ED)   // canvas
    val Ink = Color(0xFF1A1A1A)     // text, every border, Focus mode, MEDIUM urgency
    val Orange = Color(0xFFD14D2A)  // Panic, HIGH urgency, high risk, alerts
    val Green = Color(0xFF2A6B5E)   // Planning, safe risk, calendar/export
    val Purple = Color(0xFF6B5BD1)  // Review
    val Amber = Color(0xFFC99A2E)   // medium risk
    val Gray = Color(0xFF6B7280)    // LOW urgency, secondary text
    val Surface = Color(0xFFFFFFFF) // cards, top bar
    val White = Color(0xFFFFFFFF)
}

fun Mode.color(): Color = when (this) {
    Mode.PLANNING -> Brand.Green
    Mode.FOCUS -> Brand.Ink
    Mode.PANIC -> Brand.Orange
    Mode.REVIEW -> Brand.Purple
}

fun TaskPriority.color(): Color = when (this) {
    TaskPriority.HIGH -> Brand.Orange
    TaskPriority.MEDIUM -> Brand.Ink
    TaskPriority.LOW, TaskPriority.NONE -> Brand.Gray
}

/** `risk.risk_level`: high | medium | safe (anything else → gray). */
fun riskColor(level: String?): Color = when (level?.lowercase()) {
    "high" -> Brand.Orange
    "medium" -> Brand.Amber
    "safe", "low" -> Brand.Green
    else -> Brand.Gray
}
