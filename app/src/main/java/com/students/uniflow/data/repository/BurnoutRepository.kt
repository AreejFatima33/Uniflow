package com.students.uniflow.data.repository

import android.content.Context
import com.google.gson.Gson
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.StudyLogEntity
import com.students.uniflow.data.model.BurnoutResult
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.GeminiPrompts
import kotlinx.coroutines.delay

class BurnoutRepository(context: Context) {

    private val studyLogDao = AppDatabase.getDatabase(context).studyLogDao()
    private val gson = Gson()

    // Log a study session — call this from each feature after successful AI response
    suspend fun logStudySession(featureName: String, studyMinutes: Int) {
        val today = java.time.LocalDate.now().toString()
        studyLogDao.insertLog(
            StudyLogEntity(
                date = today,
                studyMinutes = studyMinutes,
                sessionCount = 1,
                featureUsed = featureName
            )
        )
    }

    // Analyze burnout risk using last 7 days of logs
    suspend fun analyzeBurnout(): Result<BurnoutResult> {
        return try {
            val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
            val logs = studyLogDao.getLogsAfter(sevenDaysAgo)
            val totalMinutes = studyLogDao.getTotalMinutesSince(sevenDaysAgo) ?: 0

            if (logs.isEmpty()) {
                // Not enough data yet — return low risk
                return Result.success(
                    BurnoutResult(
                        riskLevel = "Low",
                        summary = "Not enough study data yet. Keep using UniFlow and check back later!",
                        suggestions = listOf("Start using UniFlow daily to track your study patterns."),
                        encouragement = "You're off to a great start! 🌟"
                    )
                )
            }

            val prompt = GeminiPrompts.burnoutRadar(logs, totalMinutes)
            val rawResponse = sendWithRetry(prompt)
            android.util.Log.d("UNIFLOW_BURNOUT", "Gemini burnout response: $rawResponse")

            val cleanJson = cleanGeminiJson(rawResponse)
            val result = gson.fromJson(cleanJson, BurnoutResult::class.java)
            Result.success(result)

        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_BURNOUT", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun cleanGeminiJson(raw: String): String {
        return raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
            .let { s ->
                val start = s.indexOf('{')
                val end = s.lastIndexOf('}')
                if (start != -1 && end != -1 && end > start) s.substring(start, end + 1) else s
            }
    }

    private suspend fun sendWithRetry(prompt: String, maxRetries: Int = 3): String {
        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = GeminiClient.sendPrompt(prompt)
                if (response.contains("503") || response.contains("UNAVAILABLE") || response.contains("Error:")) {
                    lastError = Exception("Gemini error: $response")
                    delay(2000L * (attempt + 1))
                } else {
                    return response
                }
            } catch (e: Exception) {
                lastError = e
                delay(2000L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Gemini failed after $maxRetries retries")
    }
}