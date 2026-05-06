package com.students.uniflow.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.students.uniflow.data.model.TimetableEntry
import java.util.Calendar

object AlarmHelper {

    private const val CHANNEL_ID = "uniflow_reminders"

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "UniFlow Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Class and study reminders" }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    fun scheduleClassReminders(context: Context, entries: List<TimetableEntry>) {
        entries.forEachIndexed { index, entry ->
            scheduleWeeklyAlarm(context, entry, index)
        }
    }

    fun scheduleOneTimeReminder(context: Context, taskName: String, triggerAtMillis: Long): Boolean {
        return try {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    android.util.Log.w("UNIFLOW_ALARM", "Exact alarm permission not granted")
                    return false
                }
            }

            val prefs = context.getSharedPreferences("alarm_prefs", Context.MODE_PRIVATE)
            val requestCode = prefs.getInt("alarm_counter", 0) + 1
            prefs.edit().putInt("alarm_counter", requestCode).apply()

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.students.uniflow.ALARM_$requestCode"  // ← HERE inside this block
                putExtra("subject", taskName)
                putExtra("room", "")
                putExtra("time", "")
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )

            // Save for boot recovery
            val alarmsJson = prefs.getString("saved_alarms", "[]") ?: "[]"
            val alarmsList = alarmsJson.removeSurrounding("[", "]")
                .split(",")
                .filter { it.isNotBlank() }
                .toMutableList()
            alarmsList.add("$requestCode|$taskName|$triggerAtMillis")
            prefs.edit().putString("saved_alarms", "[${alarmsList.joinToString(",")}]").apply()

            android.util.Log.d("UNIFLOW_ALARM", "Alarm #$requestCode scheduled for $taskName at $triggerAtMillis")
            true
        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_ALARM", "Failed to schedule alarm: ${e.message}")
            false
        }
    }

    private fun scheduleWeeklyAlarm(context: Context, entry: TimetableEntry, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("subject", entry.subject)
            putExtra("room", entry.room)
            putExtra("time", entry.time)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val calendar = buildCalendar(entry.day, entry.time) ?: return
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,
            pendingIntent
        )
    }

    private fun buildCalendar(day: String, time: String): Calendar? {
        return try {
            val dayOfWeek = when (day.lowercase().trim()) {
                "monday"    -> Calendar.MONDAY
                "tuesday"   -> Calendar.TUESDAY
                "wednesday" -> Calendar.WEDNESDAY
                "thursday"  -> Calendar.THURSDAY
                "friday"    -> Calendar.FRIDAY
                "saturday"  -> Calendar.SATURDAY
                else        -> Calendar.SUNDAY
            }
            val parts = time.replace("AM", "").replace("PM", "").trim().split(":")
            var hour = parts[0].trim().toInt()
            val minute = parts[1].trim().toInt()
            if (time.contains("PM", ignoreCase = true) && hour != 12) hour += 12
            if (time.contains("AM", ignoreCase = true) && hour == 12) hour = 0
            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }
        } catch (e: Exception) { null }
    }
}

// AlarmReceiver — no changes needed here, action line does NOT go here
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: "Reminder"
        val room    = intent.getStringExtra("room")    ?: ""
        val time    = intent.getStringExtra("time")    ?: ""

        val notification = androidx.core.app.NotificationCompat.Builder(context, "uniflow_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UniFlow Reminder: $subject")
            .setContentText(if (time.isNotEmpty() && room.isNotEmpty()) "$time • Room $room"
            else if (time.isNotEmpty()) time
            else subject)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}