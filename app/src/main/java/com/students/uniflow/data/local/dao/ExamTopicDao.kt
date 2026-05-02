package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.ExamTopicEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamTopicDao {

    // Used by ExamOracleRepository
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: ExamTopicEntity)

    // Used by ExamOracleRepository (Flow for live updates)
    @Query("SELECT * FROM exam_topics ORDER BY createdAt DESC")
    fun getAllTopicsFlow(): Flow<List<ExamTopicEntity>>

    // Used by HistoryFragment (suspend for one-time read)
    @Query("SELECT * FROM exam_topics ORDER BY createdAt DESC")
    suspend fun getAllTopics(): List<ExamTopicEntity>

    @Query("DELETE FROM exam_topics")
    suspend fun clearAll()
}