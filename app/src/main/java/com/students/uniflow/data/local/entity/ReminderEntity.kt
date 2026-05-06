package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reminders")
data class ReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val task: String,
    val date: String,
    val time: String,
    val triggerAtMillis: Long,
    val createdAt: Long = System.currentTimeMillis()
)