package com.students.uniflow.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "concepts")
data class ConceptEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val conceptTitle: String,
    val simpleExplanation: String,
    val realLifeAnalogy: String,
    val keyPointsJson: String,   // JSON array as string
    val examTip: String,
    val savedAt: Long = System.currentTimeMillis()
)