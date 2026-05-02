package com.students.uniflow.data.remote

import com.google.ai.client.generativeai.GenerativeModel
import com.students.uniflow.BuildConfig

object GeminiClient {

    // This creates the AI model using your API key from local.properties
    private val model = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )
    // Call this function with any text prompt — it returns AI response as String
    suspend fun sendPrompt(prompt: String): String {
        return try {
            val response = model.generateContent(prompt)
            response.text ?: "No response from Gemini"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}