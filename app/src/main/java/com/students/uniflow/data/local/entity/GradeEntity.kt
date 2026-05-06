package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grades")
data class GradeEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val quizMarks: Double,
    val quizTotal: Double,
    val assignmentMarks: Double,
    val assignmentTotal: Double,
    val midtermMarks: Double,
    val midtermTotal: Double,
    val currentPercentage: Double,
    val savedAt: Long = System.currentTimeMillis()
)