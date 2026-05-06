package com.students.uniflow.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.ExamTopicEntity
import com.students.uniflow.data.model.ExamOracleResult
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.CacheHelper
import com.students.uniflow.utils.GeminiPrompts
import com.students.uniflow.utils.OcrHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class ExamOracleRepository(context: Context) {

    private val examTopicDao = AppDatabase.getInstance(context).examTopicDao()
    private val gson = Gson()
    private val appContext = context.applicationContext

    // Full pipeline: URI -> OCR -> Gemini -> JSON -> Room DB
    suspend fun processExamPaper(imageUri: Uri, paperName: String = ""): Result<ExamOracleResult> {
        return try {
            val extractedText = extractTextFromUri(imageUri)
            android.util.Log.d("UNIFLOW_OCR", "ExamOracle extracted: $extractedText")
            if (extractedText.isEmpty())
                return Result.failure(Exception("No text found in image"))

            val meaningfulWords = extractedText.split("\\s+".toRegex())
                .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            if (meaningfulWords.size < 10) {
                return Result.failure(Exception("Could not read the image clearly. Please use an English exam paper, or ensure the image is well-lit and in focus."))
            }

            // Step 2: Send to Gemini with retry
            val prompt = GeminiPrompts.examOracle(extractedText)
            val rawResponse = CacheHelper.getCached(appContext, extractedText, "exam")
                ?: sendWithRetry(prompt).also {
                    CacheHelper.saveCache(appContext, extractedText, "exam", it)
                }

            // Step 3: Clean JSON
            val cleanJson = cleanJson(rawResponse, useObject = true)

            // Step 4: Parse
            val result = gson.fromJson(cleanJson, ExamOracleResult::class.java)

            // Step 5: Save each topic to Room DB
            result.predictedTopics.forEach { topic ->
                examTopicDao.insertTopic(
                    ExamTopicEntity(
                        topic = topic.topic,
                        probability = topic.probability,
                        questionsJson = gson.toJson(topic.practiceQuestions)
                    )
                )
            }

            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllTopics(): Flow<List<ExamTopicEntity>> = examTopicDao.getAllTopicsFlow()

    // Shared JSON cleaner — useObject=true for {}, false for []
    private fun cleanJson(raw: String, useObject: Boolean): String {
        val cleaned = raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
        return if (useObject) {
            val start = cleaned.indexOf('{')
            val end = cleaned.lastIndexOf('}')
            if (start != -1 && end != -1 && end > start) cleaned.substring(start, end + 1)
            else cleaned
        } else {
            val start = cleaned.indexOf('[')
            val end = cleaned.lastIndexOf(']')
            if (start != -1 && end != -1 && end > start) cleaned.substring(start, end + 1)
            else cleaned
        }
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