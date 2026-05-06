package com.students.uniflow.data.repository

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.ReminderEntity
import com.students.uniflow.data.model.ReminderResult
import com.students.uniflow.data.remote.GeminiClient
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.utils.GeminiPrompts
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class ReminderRepository(private val context: Context) {

    private val reminderDao = AppDatabase.getInstance(context).reminderDao()
    private val gson = Gson()

    suspend fun processVoiceInput(spokenText: String): ReminderResult {
        Log.d("UNIFLOW_VOICE", "Processing: $spokenText")

        val prompt = GeminiPrompts.voiceReminder(spokenText)
        val rawResponse = try {
            GeminiClient.sendPrompt(prompt)
        } catch (e: Exception) {
            // Wait 3 seconds and try once more
            android.util.Log.w("UNIFLOW_VOICE", "Gemini failed, retrying in 3s...")
            kotlinx.coroutines.delay(3000)
            GeminiClient.sendPrompt(prompt)
        }
        Log.d("UNIFLOW_GEMINI", "Voice Gemini response: $rawResponse")

        // Strip markdown fences if present
        val cleanJson = rawResponse
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()

        val parsed = try {
            gson.fromJson(cleanJson, ReminderJsonResponse::class.java)
        } catch (e: Exception) {
            Log.e("UNIFLOW_VOICE", "JSON parse failed: ${e.message}")
            throw Exception("Could not understand the reminder. Please try again.")
        }

        // Convert date + time to milliseconds for AlarmManager
        val triggerMillis = try {
            val date = LocalDate.parse(parsed.date, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val time = LocalTime.parse(parsed.time, DateTimeFormatter.ofPattern("HH:mm"))
            val dateTime = LocalDateTime.of(date, time)
            dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (e: Exception) {
            Log.e("UNIFLOW_VOICE", "Date/time parse failed: ${e.message}")
            throw Exception("Could not parse the date or time. Please try again.")
        }

        // Check if the time is in the past
        val now = System.currentTimeMillis()
        if (triggerMillis <= now) {
            throw Exception("That time has already passed. Please set a future reminder.")
        }
// Warn if alarm is less than 3 minutes away — may not fire if app takes time to close
        if (triggerMillis - now < 3 * 60 * 1000) {
            android.util.Log.w("UNIFLOW_VOICE", "Warning: alarm set less than 3 minutes from now")
        }

        // Schedule the alarm
        val alarmSet = AlarmHelper.scheduleOneTimeReminder(context, parsed.task, triggerMillis)
        if (!alarmSet) throw Exception("Failed to schedule alarm. Please try again.")

        // Save to Room DB
        reminderDao.insertReminder(
            ReminderEntity(
                task = parsed.task,
                date = parsed.date,
                time = parsed.time,
                triggerAtMillis = triggerMillis
            )
        )
        Log.d("UNIFLOW_VOICE", "Reminder saved and alarm set for: ${parsed.date} ${parsed.time}")

        // Build a human-readable confirmation
        val displayText = "Reminder set: \"${parsed.task}\" on ${parsed.date} at ${parsed.time}"

        return ReminderResult(
            task = parsed.task,
            date = parsed.date,
            time = parsed.time,
            displayText = displayText
        )
    }

    private data class ReminderJsonResponse(
        val task: String,
        val date: String,
        val time: String
    )
}