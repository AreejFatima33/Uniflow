package com.students.uniflow.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.NoteEntity
import com.students.uniflow.data.model.NoteResult
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.GeminiPrompts
import com.students.uniflow.utils.OcrHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LectureRepository(context: Context) {

    private val noteDao = AppDatabase.getDatabase(context).noteDao()
    private val gson = Gson()
    private val appContext = context.applicationContext

    // Full pipeline: URI → OCR → Gemini → NoteResult
    suspend fun processLectureImage(imageUri: Uri): Result<NoteResult> {
        return try {
            // Step 1: OCR
            val extractedText = extractTextFromUri(imageUri)
            android.util.Log.d("UNIFLOW_OCR", "Extracted text: $extractedText")
            if (extractedText.isEmpty()) return Result.failure(Exception("No text found in image"))

            val meaningfulWords = extractedText.split("\\s+".toRegex())
                .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            if (meaningfulWords.size < 10) {
                return Result.failure(
                    Exception("Could not read the image clearly. Please ensure the image is well-lit and in focus.")
                )
            }

            // Step 2: Gemini (with retry)
            val prompt = GeminiPrompts.lectureSnap(extractedText)
            val rawResponse = sendWithRetry(prompt)
            android.util.Log.d("UNIFLOW_GEMINI", "Gemini raw response: $rawResponse")

            // Step 3: Parse JSON
            val cleanJson = rawResponse
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
                .let { s ->
                    val start = s.indexOf('{')
                    val end = s.lastIndexOf('}')
                    if (start != -1 && end != -1 && end > start) s.substring(start, end + 1) else s
                }
            val noteResult = gson.fromJson(cleanJson, NoteResult::class.java)

            // Step 4: Save to Room DB — now saves full result including keyPoints, flashcards, quiz
            noteDao.insert(
                NoteEntity(
                    title = noteResult.title,
                    summary = noteResult.summary,
                    keyPointsJson = gson.toJson(noteResult.keyPoints),
                    flashcardsJson = gson.toJson(noteResult.flashcards),
                    quizJson = gson.toJson(noteResult.quizQuestions),
                    savedAt = System.currentTimeMillis()
                )
            )
            android.util.Log.d("UNIFLOW_LECTURE", "Note saved to DB ✅")

            Result.success(noteResult)

        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_LECTURE", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    // ── Retry helper ──────────────────────────────────────────────────────────
    private suspend fun sendWithRetry(prompt: String, maxRetries: Int = 3): String {
        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = GeminiClient.sendPrompt(prompt)
                if (response.contains("503") ||
                    response.contains("UNAVAILABLE") ||
                    response.contains("Error:")
                ) {
                    lastError = Exception("Gemini API error: $response")
                    val delayMs = 2000L * (attempt + 1)
                    android.util.Log.d("UNIFLOW_GEMINI", "Retry ${attempt + 1} after ${delayMs}ms")
                    delay(delayMs)
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

    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotesFlow()

    // Wraps the callback-based OcrHelper into a suspend function
    private suspend fun extractTextFromUri(uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            OcrHelper.extractTextFromImage(
                appContext, uri,
                onSuccess = { text -> cont.resume(text) },
                onError   = { _    -> cont.resume("") }
            )
        }
}