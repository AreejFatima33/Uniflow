package com.students.uniflow.utils

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.students.uniflow.data.model.TimetableEntry
import java.util.Calendar

object AlarmHelper {

    private const val CHANNEL_ID = "uniflow_reminders"

    // Call once at app start to create the notification channel
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "UniFlow Reminders",
            NotificationManager.IMPORTANCE_HIGH
        ).apply { description = "Class and study reminders" }
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    // Schedule weekly recurring alarms for all timetable entries
    fun scheduleClassReminders(context: Context, entries: List<TimetableEntry>) {
        entries.forEachIndexed { index, entry ->
            scheduleWeeklyAlarm(context, entry, index)
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

        // Parse day + time into a Calendar
        val calendar = buildCalendar(entry.day, entry.time) ?: return

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY * 7,   // repeat weekly
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
                else         -> Calendar.SUNDAY
            }
            // Parse "09:00 AM" or "14:30"
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
                // If time already passed this week, schedule next week
                if (timeInMillis < System.currentTimeMillis()) add(Calendar.WEEK_OF_YEAR, 1)
            }
        } catch (e: Exception) { null }
    }
}

// Receives the alarm and shows the notification
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject = intent.getStringExtra("subject") ?: "Class"
        val room    = intent.getStringExtra("room")    ?: ""
        val time    = intent.getStringExtra("time")    ?: ""

        val notification = NotificationCompat.Builder(context, "uniflow_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 Class Reminder: $subject")
            .setContentText("$time${if (room.isNotEmpty()) " • Room $room" else ""}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
