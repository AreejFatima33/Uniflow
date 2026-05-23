package com.students.uniflow.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.model.TimetableEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("UNIFLOW_ALARM", "Boot detected — rescheduling all alarms")

        AlarmHelper.createNotificationChannel(context)

        // 1. Restore timetable class alarms from Room DB
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Inside BootReceiver.kt Coroutine Block:
                val entries = AppDatabase.getInstance(context)
                    .timetableDao()
                    .getAllEntriesList()
                    .map {
                        TimetableEntry(
                            day       = it.day,
                            time      = it.time,
                            subject   = it.subject,
                            room      = it.room,
                            professor = it.professor
                        )
                    }

                    .filter { !it.subject.equals("Break", ignoreCase = true) && it.time.isNotBlank() }

                if (entries.isNotEmpty()) {
                    AlarmHelper.scheduleClassReminders(context, entries)
                    Log.d("UNIFLOW_ALARM", "Rescheduled ${entries.size} class alarms after boot")
                }
            } catch (e: Exception) {
                Log.e("UNIFLOW_ALARM", "Failed to restore class alarms: ${e.message}")
            }
        }

        // 2. Restore VoiceReminder one-time alarms from SharedPreferences
        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString("saved_alarms", "[]") ?: "[]"
        val now = System.currentTimeMillis()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val stillValid = mutableListOf<String>()

        val alarmsList = alarmsJson.removeSurrounding("[", "]")
            .split(",")
            .filter { it.isNotBlank() }

        for (entry in alarmsList) {
            val parts = entry.split("|")
            if (parts.size != 3) continue

            val requestCode  = parts[0].toIntOrNull() ?: continue
            val taskName     = parts[1]
            val triggerMillis = parts[2].toLongOrNull() ?: continue

            if (triggerMillis <= now) {
                Log.d("UNIFLOW_ALARM", "Skipping past voice alarm: $taskName")
                continue
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.students.uniflow.ALARM_$requestCode"
                putExtra("subject", taskName)
                putExtra("room", "")
                putExtra("time", "")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, requestCode, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent
                )
                stillValid.add(entry)
                Log.d("UNIFLOW_ALARM", "Rescheduled voice alarm: $taskName")
            } catch (e: Exception) {
                Log.e("UNIFLOW_ALARM", "Failed to reschedule: ${e.message}")
            }
        }

        prefs.edit()
            .putString("saved_alarms", "[${stillValid.joinToString(",")}]")
            .apply()
    }
}