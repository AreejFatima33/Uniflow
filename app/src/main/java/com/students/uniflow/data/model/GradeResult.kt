package com.students.uniflow.data.model

data class GradeResult(
    val currentPercentage: Double,
    val requiredForA: Double,
    val requiredForB: Double,
    val requiredForC: Double,
    val grade: String           // current letter grade
)