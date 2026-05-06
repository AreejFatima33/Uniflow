package com.students.uniflow.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.StudyPlanEntity
import com.students.uniflow.data.model.StudyPlanResult
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.GeminiPrompts
import com.students.uniflow.utils.OcrHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class StudyPlanRepository(context: Context) {

    private val studyPlanDao = AppDatabase.getInstance(context).studyPlanDao()
    private val gson = Gson()
    private val appContext = context.applicationContext

    // Full pipeline: URI -> OCR -> Gemini -> JSON -> Room DB
    suspend fun processSyllabus(
        imageUri: Uri,
        examDate: String,
        subjectName: String
    ): Result<StudyPlanResult> {
        return try {
            // Step 1: Extract text from syllabus photo
            val extractedText = extractTextFromUri(imageUri)
            android.util.Log.d("UNIFLOW_OCR", "DeadlineGenie extracted: $extractedText")
            if (extractedText.isEmpty())
                return Result.failure(Exception("No text found in image"))

// Quality check — reject garbled/Urdu text before calling Gemini
            val meaningfulWords = extractedText.split("\\s+".toRegex())
                .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            if (meaningfulWords.size < 10) {
                return Result.failure(Exception("Could not read the image clearly. Please use an English syllabus, or ensure the image is well-lit and in focus."))
            }

            // Step 2: Send to Gemini
            val prompt = GeminiPrompts.deadlineGenie(extractedText, examDate, subjectName)
            val rawResponse = sendWithRetry(prompt)
            android.util.Log.d("UNIFLOW_GEMINI", "DeadlineGenie response: $rawResponse")

            // Step 3: Clean JSON
            val cleanJson = cleanJson(rawResponse)

            // Step 4: Parse
            val result = gson.fromJson(cleanJson, StudyPlanResult::class.java)

            // Step 5: Clear old plan and save new one to Room DB
            studyPlanDao.clearAll()
            result.dailyPlan.forEach { day ->
                studyPlanDao.insertDay(
                    StudyPlanEntity(
                        day = day.day,
                        date = day.date,
                        topic = day.topic,
                        tasksJson = gson.toJson(day.tasks)
                    )
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllDays(): Flow<List<StudyPlanEntity>> = studyPlanDao.getAllDays()

    private fun cleanJson(raw: String): String {
        val cleaned = raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        val start = cleaned.indexOf('{')
        val end = cleaned.lastIndexOf('}')
        return if (start != -1 && end != -1 && end > start)
            cleaned.substring(start, end + 1) else cleaned
    }

    private suspend fun sendWithRetry(prompt: String, maxRetries: Int = 3): String {
        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = GeminiClient.sendPrompt(prompt)
                if (response.contains("503") ||
                    response.contains("UNAVAILABLE") ||
                    response.contains("Error:")
                ) {
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

    private suspend fun extractTextFromUri(uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            OcrHelper.extractTextFromImage(
                appContext, uri,
                onSuccess = { text -> cont.resume(text) },
                onError = { _ -> cont.resume("") }
            )
        }
}