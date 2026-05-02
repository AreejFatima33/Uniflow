package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.ConceptEntity

@Dao
interface ConceptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(concept: ConceptEntity)

    @Query("SELECT * FROM concepts ORDER BY savedAt DESC")
    suspend fun getAll(): List<ConceptEntity>

    @Query("SELECT * FROM concepts WHERE id = :id")
    suspend fun getById(id: Int): ConceptEntity?

    @Query("DELETE FROM concepts WHERE id = :id")
    suspend fun deleteById(id: Int)
}