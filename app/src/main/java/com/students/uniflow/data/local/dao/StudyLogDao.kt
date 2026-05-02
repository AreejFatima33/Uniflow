package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.StudyLogEntity

@Dao
interface StudyLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: StudyLogEntity)

    // Get logs from last 7 days for burnout analysis
    @Query("SELECT * FROM study_logs WHERE timestamp >= :sinceTimestamp ORDER BY timestamp DESC")
    suspend fun getLogsAfter(sinceTimestamp: Long): List<StudyLogEntity>

    // Get total study minutes in last 7 days
    @Query("SELECT SUM(studyMinutes) FROM study_logs WHERE timestamp >= :sinceTimestamp")
    suspend fun getTotalMinutesSince(sinceTimestamp: Long): Int?

    @Query("SELECT * FROM study_logs ORDER BY timestamp DESC LIMIT 30")
    suspend fun getRecentLogs(): List<StudyLogEntity>
}