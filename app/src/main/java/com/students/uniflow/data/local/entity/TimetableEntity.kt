package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "timetable")
data class TimetableEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val day: String,         // e.g. "Monday"
    val time: String,        // e.g. "09:00 AM"
    val subject: String,
    val room: String,
    val professor: String = "",
    val timestamp: Long = System.currentTimeMillis()
)