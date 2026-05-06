package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.GradeEntity

@Dao
interface GradeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGrade(grade: GradeEntity)

    @Query("SELECT * FROM grades ORDER BY savedAt DESC")
    suspend fun getAllGrades(): List<GradeEntity>

    @Query("DELETE FROM grades")
    suspend fun clearAll()
}