package com.students.uniflow

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.worker.BurnoutWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lifecycleScope.launch {
            com.students.uniflow.utils.CacheHelper.clearOldCache(applicationContext)
        }

        AlarmHelper.createNotificationChannel(this)

        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setupWithNavController(navController)

        bottomNav.setOnItemSelectedListener { item ->
            bottomNav.menu.findItem(item.itemId)?.isChecked = true
            navController.navigate(item.itemId)
            true
        }

        scheduleBurnoutWorker()

        // Request all background permissions on first launch
        requestBackgroundPermissions()
    }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            android.util.Log.d("UNIFLOW", "Notification permission granted")
        } else {
            // User denied — show explanation and take them to settings
            AlertDialog.Builder(this)
                .setTitle("Notifications Disabled")
                .setMessage(
                    "UniFlow needs notification permission to send reminders.\n\n" +
                            "Please go to Settings → Apps → UniFlow → Notifications and enable them."
                )
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                    )
                }
                .setNegativeButton("Skip", null)
                .show()
        }
    }

    private fun requestBackgroundPermissions() {
        val prefs = getSharedPreferences("uniflow_prefs", MODE_PRIVATE)
        val alreadyAsked = prefs.getBoolean("permissions_requested", false)
        if (alreadyAsked) return
        prefs.edit().putBoolean("permissions_requested", true).apply()

        // Step 1 — Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val notifGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!notifGranted) {
                AlertDialog.Builder(this)
                    .setTitle("Enable Notifications")
                    .setMessage(
                        "UniFlow needs to send you reminders.\n\n" +
                                "On the next screen, tap Allow to receive your reminders on time."
                    )
                    .setPositiveButton("Continue") { _, _ ->
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        // After notification, request alarm + battery
                        requestExactAlarmPermission()
                    }
                    .setCancelable(false)
                    .show()
                return
            }
        }

        // Already have notification permission — proceed to alarm + battery
        requestExactAlarmPermission()
    }

    private fun requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(android.app.AlarmManager::class.java)
            if (!alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Allow Exact Alarms")
                    .setMessage(
                        "On the next screen, find UniFlow and tap to allow alarms.\n\n" +
                                "This lets your reminders ring at exactly the right time."
                    )
                    .setPositiveButton("Open Settings") { _, _ ->
                        startActivity(
                            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                data = Uri.parse("package:$packageName")
                            }
                        )
                        // After alarm permission, request battery optimization
                        requestBatteryOptimization()
                    }
                    .setCancelable(false)
                    .show()
            } else {
                requestBatteryOptimization()
            }
        } else {
            requestBatteryOptimization()
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Keep Reminders Active")
                    .setMessage(
                        "On the next screen, find UniFlow and select " +
                                "\"Don't optimize\" or \"No restrictions\".\n\n" +
                                "This ensures your reminders work even when the app is closed."
                    )
                    .setPositiveButton("Open Settings") { _, _ ->
                        try {
                            startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:$packageName")
                                }
                            )
                        } catch (e: Exception) {
                            // Fallback for devices that block direct request (some Vivo/MIUI)
                            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    }
                    .setCancelable(false)
                    .show()
            }
        }
    }

    private fun scheduleBurnoutWorker() {
        val burnoutRequest = PeriodicWorkRequestBuilder<BurnoutWorker>(
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "burnout_daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            burnoutRequest
        )

        android.util.Log.d("UNIFLOW_BURNOUT", "BurnoutWorker scheduled daily ✅")
    }
}