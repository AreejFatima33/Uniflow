package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.StudyPlanEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudyPlanDao {

    // Used by StudyPlanRepository
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDay(plan: StudyPlanEntity)

    // Used by StudyPlanRepository (Flow for live updates)
    @Query("SELECT * FROM study_plan ORDER BY day ASC")
    fun getAllDays(): Flow<List<StudyPlanEntity>>

    // Used by HistoryFragment (suspend for one-time read)
    @Query("SELECT * FROM study_plan ORDER BY createdAt DESC")
    suspend fun getAllPlans(): List<StudyPlanEntity>

    @Query("DELETE FROM study_plan")
    suspend fun clearAll()
}