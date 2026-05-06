package com.students.uniflow.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.students.uniflow.data.local.entity.GeminiCacheEntity

@Dao
interface GeminiCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCache(cache: GeminiCacheEntity)

    @Query("SELECT * FROM gemini_cache WHERE inputHash = :hash AND featureName = :feature LIMIT 1")
    suspend fun getCached(hash: String, feature: String): GeminiCacheEntity?

    // Delete cache older than 7 days to prevent stale results
    @Query("DELETE FROM gemini_cache WHERE savedAt < :cutoff")
    suspend fun deleteOldCache(cutoff: Long)
}