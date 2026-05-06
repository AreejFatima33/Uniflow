package com.students.uniflow.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.TimetableEntity
import com.students.uniflow.data.model.TimetableEntry
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.utils.CacheHelper
import com.students.uniflow.utils.GeminiPrompts
import com.students.uniflow.utils.OcrHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class TimetableRepository(context: Context) {

    private val timetableDao = AppDatabase.getInstance(context).timetableDao()
    private val gson = Gson()
    private val appContext = context.applicationContext

    // Full pipeline: URI → OCR → Gemini → save → schedule alarms
    suspend fun processTimetableImage(imageUri: Uri): Result<List<TimetableEntry>> {
        return try {
            // Step 1: OCR
            val extractedText = extractTextFromUri(imageUri)
            if (extractedText.isEmpty()) return Result.failure(Exception("No text found in image"))

            val meaningfulWords = extractedText.split("\\s+".toRegex())
                .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            if (meaningfulWords.size < 10) {
                return Result.failure(Exception("Could not read the image clearly. Please ensure the timetable is in English and the image is clear."))
            }


            // Step 2: Gemini (with cache)
            val prompt = GeminiPrompts.timetableSnap(extractedText)
            val rawResponse = CacheHelper.getCached(appContext, extractedText, "timetable")
                ?: GeminiClient.sendPrompt(prompt).also { response ->
                    CacheHelper.saveCache(appContext, extractedText, "timetable", response)
                }

            // Step 3: Parse JSON array
            val cleanJson = rawResponse
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
                .let { s ->
                    // For timetable, find first [ and last ]
                    val start = s.indexOf('[')
                    val end = s.lastIndexOf(']')
                    if (start != -1 && end != -1 && end > start) s.substring(start, end + 1)
                    else s
                }
            val type = object : TypeToken<List<TimetableEntry>>() {}.type
            val entries: List<TimetableEntry> = gson.fromJson(cleanJson, type)

            // Step 4: Save to Room DB (replace all previous entries)
            timetableDao.deleteAll()
            timetableDao.insertAll(entries.map {
                TimetableEntity(day = it.day, time = it.time, subject = it.subject,
                    room = it.room, professor = it.professor)
            })

            // Step 5: Schedule weekly alarms for every class
            AlarmHelper.scheduleClassReminders(appContext, entries)

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getAllEntries(): Flow<List<TimetableEntity>> = timetableDao.getAllEntries()

    private suspend fun extractTextFromUri(uri: Uri): String =
        suspendCancellableCoroutine { cont ->
            OcrHelper.extractTextFromImage(
                appContext, uri,
                onSuccess = { text -> cont.resume(text) },
                onError   = { _    -> cont.resume("") }
            )
        }
}
