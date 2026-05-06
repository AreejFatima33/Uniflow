package com.students.uniflow.data.model

data class ReminderResult(
    val task: String,
    val date: String,           // e.g. "2026-05-03"
    val time: String,           // e.g. "18:00"
    val displayText: String     // e.g. "Reminder set for tomorrow at 6:00 PM"
)