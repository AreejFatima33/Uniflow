package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gemini_cache")
data class GeminiCacheEntity(
    @PrimaryKey
    val inputHash: String,        // MD5 hash of the input (OCR text or image bytes)
    val featureName: String,      // "lecture", "timetable", "exam", "concept"
    val cachedResponse: String,   // Raw JSON response from Gemini
    val savedAt: Long = System.currentTimeMillis()
)