package com.students.uniflow.data.model

data class BurnoutResult(
    val riskLevel: String,          // "High", "Medium", "Low"
    val summary: String,            // short explanation from Gemini
    val suggestions: List<String>,  // actionable advice
    val encouragement: String       // a caring message for the student
)