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

    suspend fun processTimetableImage(imageUri: Uri): Result<List<TimetableEntry>> {
        return try {
            // Step 1: OCR
            val extractedText = extractTextFromUri(imageUri)
            if (extractedText.isEmpty())
                return Result.failure(Exception("No text found in image"))

            val meaningfulWords = extractedText.split("\\s+".toRegex())
                .filter { it.length > 2 && it.all { c -> c.isLetter() } }
            if (meaningfulWords.size < 10) {
                return Result.failure(
                    Exception("Could not read the image clearly. Please ensure the timetable is in English and the image is clear.")
                )
            }


            // Step 2: Gemini — try all keys, only cache on success
            val prompt = GeminiPrompts.timetableSnap(extractedText)
            val rawResponse = try {
                val response = GeminiClient.sendPrompt(prompt)
                // Only cache if response looks valid (starts with [ )
                val trimmed = response.trim()
                if (trimmed.startsWith("[")) {
                    CacheHelper.saveCache(appContext, extractedText, "timetable", response)
                }
                response
            } catch (e: Exception) {
                android.util.Log.e("UNIFLOW_TIMETABLE", "All Gemini keys failed: ${e.message}")
                return Result.failure(Exception("Could not process timetable. Please try again with a clearer image."))
            }

            // Step 3: Parse JSON array
            val cleanJson = rawResponse
                .replace(Regex("```json\\s*"), "")
                .replace(Regex("```\\s*"), "")
                .trim()
                .let { s ->
                    val start = s.indexOf('[')
                    val end   = s.lastIndexOf(']')
                    if (start != -1 && end != -1 && end > start) s.substring(start, end + 1)
                    else s
                }

            if (cleanJson.isBlank() || cleanJson.first() != '[') {
                android.util.Log.e("UNIFLOW_TIMETABLE", "Invalid Gemini response after cleaning")
                return Result.failure(Exception("AI could not read the timetable. Please try again with a clearer image."))
            }

            val type = object : TypeToken<List<TimetableEntry>>() {}.type
            val rawEntries: List<TimetableEntry> = gson.fromJson(cleanJson, type)
            // Step 4: Validate — deduplicate and remove blank entries
            val validatedEntries = rawEntries
                .filter { it.day.isNotBlank() && it.subject.isNotBlank() }
                .distinctBy {
                    "${it.day.trim().lowercase()}_${it.time.trim()}_${it.subject.trim().lowercase()}"
                }

            // Step 5: Save validated entries to Room DB
            timetableDao.deleteAll()
            timetableDao.insertAll(validatedEntries.map {
                TimetableEntity(
                    day       = it.day,
                    time      = it.time,
                    subject   = it.subject,
                    room      = it.room,
                    professor = it.professor
                )
            })

            val actualClassesOnly = validatedEntries.filter {
                it.subject.isNotBlank() &&
                        !it.subject.equals("Break", ignoreCase = true) &&
                        it.time.isNotBlank()
            }

            AlarmHelper.createNotificationChannel(appContext)

            AlarmHelper.scheduleClassReminders(appContext, actualClassesOnly)

            val classCount = actualClassesOnly.size
            val notificationManager = appContext.getSystemService(
                android.content.Context.NOTIFICATION_SERVICE
            ) as android.app.NotificationManager

            val notification = androidx.core.app.NotificationCompat.Builder(
                appContext, "uniflow_reminders"
            )
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("✅ Timetable Saved!")
                .setContentText("$classCount classes found. Weekly reminders set 10 min before each class.")
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()

            notificationManager.notify(99999, notification)

            android.util.Log.d("UNIFLOW_TIMETABLE", "Saved ${validatedEntries.size} entries, alarms scheduled")
            Result.success(validatedEntries)

        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_TIMETABLE", "Failed: ${e.message}")
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