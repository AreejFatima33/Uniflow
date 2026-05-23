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

    // ── Timetable: weekly repeating alarms ────────────────────────────────

    fun scheduleClassReminders(context: Context, entries: List<TimetableEntry>) {
        createNotificationChannel(context)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                android.util.Log.w("UNIFLOW_ALARM",
                    "Exact alarm permission not granted — reminders may be delayed")
            }
        }

        entries.forEachIndexed { index, entry ->
            scheduleWeeklyAlarm(context, entry, index + 1000)
        }
    }

    private fun scheduleWeeklyAlarm(context: Context, entry: TimetableEntry, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = buildCalendarInternal(entry.day, entry.time) ?: return

        calendar.add(Calendar.MINUTE, -10)

        // If already passed this week, schedule for next week
        if (calendar.timeInMillis < System.currentTimeMillis()) {
            calendar.add(Calendar.WEEK_OF_YEAR, 1)
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = "com.students.uniflow.CLASS_ALARM_$requestCode"
            putExtra("subject",      entry.subject)
            putExtra("room",         entry.room)
            putExtra("time",         entry.time)
            putExtra("day",          entry.day)
            putExtra("request_code", requestCode)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Exact alarm — works even in doze mode
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        } else if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }

        android.util.Log.d("UNIFLOW_ALARM",
            "Exact alarm: ${entry.subject} on ${entry.day} at ${entry.time} " +
                    "(fires ${calendar.time}, code=$requestCode)")
    }

    internal fun buildCalendarInternal(day: String, time: String): Calendar? {
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

            // Handle "09:00 AM", "9:00", "09:00-10:00" — take start time only
            val rawTime = time.split("-").first().trim()
            val isPm = rawTime.contains("PM", ignoreCase = true)
            val isAm = rawTime.contains("AM", ignoreCase = true)
            val cleaned = rawTime.replace("AM", "", ignoreCase = true)
                .replace("PM", "", ignoreCase = true).trim()
            val parts = cleaned.split(":")
            var hour = parts[0].trim().toInt()
            val minute = if (parts.size > 1) parts[1].trim().toInt() else 0

            if (isPm && hour != 12) hour += 12
            if (isAm && hour == 12) hour = 0

            // Fix: Auto-convert typical afternoon classes (e.g., 1 PM to 7 PM) if AM/PM missing
            if (!isPm && !isAm && hour in 1..7) {
                hour += 12
            }

            Calendar.getInstance().apply {
                set(Calendar.DAY_OF_WEEK, dayOfWeek)
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_ALARM", "buildCalendarInternal failed for '$day' '$time': ${e.message}")
            null
        }
    }

    fun buildNextWeekCalendar(day: String, time: String): Calendar? {
        val cal = buildCalendarInternal(day, time) ?: return null
        cal.add(Calendar.WEEK_OF_YEAR, 1)
        return cal
    }

    // ── VoiceReminder: one-time exact alarm ───────────────────────────────

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
                action = "com.students.uniflow.ALARM_$requestCode"
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
            val alarmsJson  = prefs.getString("saved_alarms", "[]") ?: "[]"
            val alarmsList  = alarmsJson.removeSurrounding("[", "]")
                .split(",").filter { it.isNotBlank() }.toMutableList()
            alarmsList.add("$requestCode|$taskName|$triggerAtMillis")
            prefs.edit().putString("saved_alarms",
                "[${alarmsList.joinToString(",")}]").apply()

            android.util.Log.d("UNIFLOW_ALARM",
                "One-time alarm #$requestCode set for '$taskName' at $triggerAtMillis")
            true
        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_ALARM", "Failed to schedule alarm: ${e.message}")
            false
        }
    }
}

// ── AlarmReceiver ─────────────────────────────────────────────────────────

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val subject     = intent.getStringExtra("subject")      ?: "Class Reminder"
        val room        = intent.getStringExtra("room")         ?: ""
        val time        = intent.getStringExtra("time")         ?: ""
        val day         = intent.getStringExtra("day")          ?: ""
        val requestCode = intent.getIntExtra("request_code", -1)

        AlarmHelper.createNotificationChannel(context)

        val bodyText = when {
            time.isNotEmpty() && room.isNotEmpty() -> "Starting at $time • Room $room"
            time.isNotEmpty()                      -> "Starting at $time"
            else                                   -> "Class reminder"
        }

        val notification = NotificationCompat.Builder(context, "uniflow_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("📚 $subject — 10 min warning")
            .setContentText(bodyText)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(requestCode, notification)

        // Reschedule for next week — exact alarms don't repeat automatically
        if (requestCode != -1 && day.isNotEmpty()) {
            val entry = com.students.uniflow.data.model.TimetableEntry(
                day = day, time = time, subject = subject, room = room
            )
            val nextWeekCalendar = AlarmHelper.buildNextWeekCalendar(day, time) ?: return
            nextWeekCalendar.add(Calendar.MINUTE, -10)

            val nextIntent = Intent(context, AlarmReceiver::class.java).apply {
                action = "com.students.uniflow.CLASS_ALARM_$requestCode"
                putExtra("subject",      subject)
                putExtra("room",         room)
                putExtra("time",         time)
                putExtra("day",          day)
                putExtra("request_code", requestCode)
            }
            val nextPending = PendingIntent.getBroadcast(
                context, requestCode, nextIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP, nextWeekCalendar.timeInMillis, nextPending
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, nextWeekCalendar.timeInMillis, nextPending
                )
            }
        }
    }
}