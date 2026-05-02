package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

// Saves each predicted exam topic to the local database
@Entity(tableName = "exam_topics")
data class ExamTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val topic: String,
    val probability: String,
    val questionsJson: String,     // Store list of questions as JSON string
    val createdAt: Long = System.currentTimeMillis()
)