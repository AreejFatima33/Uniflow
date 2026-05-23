package com.students.uniflow.data.remote

import android.util.Log
import com.google.ai.client.generativeai.GenerativeModel
import com.students.uniflow.BuildConfig

object GeminiClient {

    private const val TAG = "UNIFLOW_GEMINI"

    private val apiKeys: List<String> = listOf(
        BuildConfig.GEMINI_API_KEY,
        BuildConfig.GEMINI_API_KEY_2,
        BuildConfig.GEMINI_API_KEY_3
    ).filter { key -> key.isNotBlank() && key != "null" }

    suspend fun sendPrompt(prompt: String): String {
        if (apiKeys.isEmpty()) {
            throw Exception("No API keys configured")
        }

        for (index in apiKeys.indices) {
            val key = apiKeys[index]
            try {
                Log.d(TAG, "Trying API key ${index + 1} of ${apiKeys.size}")
                val model = GenerativeModel(
                    modelName = "gemini-2.5-flash",
                    apiKey = key
                )
                val response = model.generateContent(prompt)
                val result = response.text ?: throw Exception("Empty response from Gemini")
                Log.d(TAG, "Key ${index + 1} succeeded")
                return result

            } catch (e: Exception) {
                val message = e.message ?: ""
                val shouldTryNext = message.contains("quota", ignoreCase = true)
                        || message.contains("429")
                        || message.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
                        || message.contains("rate limit", ignoreCase = true)
                        || message.contains("503")
                        || message.contains("UNAVAILABLE", ignoreCase = true)
                        || message.contains("high demand", ignoreCase = true)
                        || message.contains("try again later", ignoreCase = true)
                        || message.contains("overloaded", ignoreCase = true)

                if (shouldTryNext && index < apiKeys.size - 1) {
                    Log.w(TAG, "Key ${index + 1} unavailable — trying next key")
                    continue
                }

                // Last key or non-retryable error — throw so callers can handle it
                Log.e(TAG, "Key ${index + 1} failed: $message")
                throw Exception(message)
            }
        }

        throw Exception("All API keys unavailable. Please try again in a moment.")
    }
}