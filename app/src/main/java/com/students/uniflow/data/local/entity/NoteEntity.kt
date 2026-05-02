package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val summary: String,
    val keyPointsJson: String,    // JSON array stored as string
    val flashcardsJson: String,   // JSON array stored as string
    val quizJson: String,         // JSON array stored as string
    val savedAt: Long = System.currentTimeMillis()
)