package com.students.uniflow.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.students.uniflow.data.repository.BurnoutRepository

class BurnoutWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "burnout_channel"
        const val NOTIFICATION_ID = 1001
    }

    override suspend fun doWork(): Result {
        android.util.Log.d("UNIFLOW_BURNOUT", "BurnoutWorker started")

        val repository = BurnoutRepository(context)
        val result = repository.analyzeBurnout()

        result.onSuccess { burnoutResult ->
            android.util.Log.d("UNIFLOW_BURNOUT", "Risk level: ${burnoutResult.riskLevel}")

            // Only send notification if Medium or High risk
            if (burnoutResult.riskLevel == "High" || burnoutResult.riskLevel == "Medium") {
                sendNotification(burnoutResult.encouragement, burnoutResult.summary)
            }
        }.onFailure { error ->
            android.util.Log.e("UNIFLOW_BURNOUT", "Worker failed: ${error.message}")
        }

        return Result.success()
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel (required for Android 8+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Burnout Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications when burnout risk is detected"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("UniFlow — Study Check-in 💙")
            .setContentText(title)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$title\n\n$message"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}