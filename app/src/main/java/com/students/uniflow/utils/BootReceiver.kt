package com.students.uniflow.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        Log.d("UNIFLOW_ALARM", "Boot detected — rescheduling alarms")

        val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
        val alarmsJson = prefs.getString("saved_alarms", "[]") ?: "[]"
        val now = System.currentTimeMillis()

        val alarmsList = alarmsJson.removeSurrounding("[", "]")
            .split(",")
            .filter { it.isNotBlank() }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val stillValid = mutableListOf<String>()

        for (entry in alarmsList) {
            val parts = entry.split("|")
            if (parts.size != 3) continue

            val requestCode = parts[0].toIntOrNull() ?: continue
            val taskName = parts[1]
            val triggerMillis = parts[2].toLongOrNull() ?: continue

            // Skip alarms that already passed
            if (triggerMillis <= now) {
                Log.d("UNIFLOW_ALARM", "Skipping past alarm: $taskName")
                continue
            }

            val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.students.uniflow.ALARM_$requestCode"
                putExtra("subject", taskName)
                putExtra("room", "")
                putExtra("time", "")
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            try {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerMillis,
                    pendingIntent
                )
                stillValid.add(entry)
                Log.d("UNIFLOW_ALARM", "Rescheduled: $taskName at $triggerMillis")
            } catch (e: Exception) {
                Log.e("UNIFLOW_ALARM", "Failed to reschedule: ${e.message}")
            }
        }

        // Save only future alarms back
        prefs.edit().putString("saved_alarms", "[${stillValid.joinToString(",")}]").apply()
    }
}