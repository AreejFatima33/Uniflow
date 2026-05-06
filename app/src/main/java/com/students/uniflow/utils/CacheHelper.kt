package com.students.uniflow.utils

import android.content.Context
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.GeminiCacheEntity
import java.security.MessageDigest

object CacheHelper {

    // Generates MD5 hash from any string input
    fun hash(input: String): String {
        val bytes = MessageDigest.getInstance("MD5").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    // Returns cached Gemini response if found, null otherwise
    suspend fun getCached(context: Context, input: String, feature: String): String? {
        val hash = hash(input)
        val cached = AppDatabase.getInstance(context).geminiCacheDao()
            .getCached(hash, feature)
        if (cached != null) {
            android.util.Log.d("UNIFLOW_CACHE", "Cache HIT for $feature ✅")
        } else {
            android.util.Log.d("UNIFLOW_CACHE", "Cache MISS for $feature — calling Gemini")
        }
        return cached?.cachedResponse
    }

    // Saves Gemini response to cache
    suspend fun saveCache(context: Context, input: String, feature: String, response: String) {
        val hash = hash(input)
        AppDatabase.getInstance(context).geminiCacheDao().insertCache(
            GeminiCacheEntity(
                inputHash = hash,
                featureName = feature,
                cachedResponse = response
            )
        )
        android.util.Log.d("UNIFLOW_CACHE", "Saved to cache: $feature ✅")
    }

    // Call this once on app start to clean up old cache
    suspend fun clearOldCache(context: Context) {
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        AppDatabase.getInstance(context).geminiCacheDao().deleteOldCache(sevenDaysAgo)
    }
}