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
            return "Error: No API keys configured"
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
                val result = response.text ?: "No response from Gemini"
                Log.d(TAG, "Key ${index + 1} succeeded")
                return result
            } catch (e: Exception) {
                val message = e.message ?: ""
                val isQuotaError = message.contains("quota", ignoreCase = true)
                        || message.contains("429")
                        || message.contains("RESOURCE_EXHAUSTED", ignoreCase = true)
                        || message.contains("rate limit", ignoreCase = true)

                if (isQuotaError && index < apiKeys.size - 1) {
                    Log.w(TAG, "Key ${index + 1} quota exceeded — trying next key")
                    continue
                }
                Log.e(TAG, "Key ${index + 1} failed: $message")
                return "Error: $message"
            }
        }

        return "Error: All API keys quota exceeded. Please try again in a minute."
    }
}