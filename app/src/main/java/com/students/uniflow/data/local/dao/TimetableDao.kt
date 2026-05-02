package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.TimetableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimetableDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<TimetableEntity>)

    @Query("DELETE FROM timetable")
    suspend fun deleteAll()

    // For TimetableRepository — returns Flow for live observation
    @Query("SELECT * FROM timetable ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<TimetableEntity>>

    // For HistoryFragment — returns List as a one-time suspend call
    @Query("SELECT * FROM timetable ORDER BY timestamp DESC")
    suspend fun getAllEntriesList(): List<TimetableEntity>
}