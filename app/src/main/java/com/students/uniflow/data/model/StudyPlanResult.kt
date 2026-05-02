package com.students.uniflow.data.model

// Gemini returns a list of daily study tasks
data class StudyPlanResult(
    val planTitle: String,
    val totalDays: Int,
    val dailyPlan: List<DayPlan>
)

data class DayPlan(
    val day: Int,           // e.g. 1, 2, 3
    val date: String,       // e.g. "Monday, April 28"
    val topic: String,      // What to study
    val tasks: List<String> // Specific tasks for that day
)