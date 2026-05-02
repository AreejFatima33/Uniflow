package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Saves each day of the study plan to the local database
@Entity(tableName = "study_plan")
data class StudyPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val day: Int,
    val date: String,
    val topic: String,
    val tasksJson: String,        // List of tasks stored as JSON string
    val createdAt: Long = System.currentTimeMillis()
)