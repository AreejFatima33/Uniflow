package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_logs")
data class StudyLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,           // "2026-04-29"
    val studyMinutes: Int,      // total study time that day in minutes
    val sessionCount: Int,      // how many times they opened the app
    val featureUsed: String,    // which feature was used e.g. "LectureSnap"
    val timestamp: Long = System.currentTimeMillis()
)