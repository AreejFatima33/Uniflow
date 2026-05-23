package com.students.uniflow

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import androidx.navigation.ui.setupWithNavController
import com.students.uniflow.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Notification permission launcher
    private val requestNotificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                // User denied — show explanation once, don't spam
                android.util.Log.w("UNIFLOW", "Notification permission denied by user")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.students.uniflow.utils.AlarmHelper.createNotificationChannel(this)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHost.navController

        binding.bottomNav.setOnItemSelectedListener { item ->
            // Check if the clicked item is the Home tab
            if (item.itemId == R.id.nav_home) {
                // Pop the backstack back to the start destination
                navController.popBackStack(R.id.nav_home, false)
                true
            } else {
                // Handle other tabs normally
                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setLaunchSingleTop(true)
                    .setRestoreState(true)
                    .setPopUpTo(navController.graph.findStartDestination().id, inclusive = false, saveState = true)
                    .build()

                try {
                    navController.navigate(item.itemId, null, navOptions)
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            binding.bottomNav.menu.findItem(destination.id)?.isChecked = true
        }

        // Step 1: Ask notification permission (Android 13+)
        requestNotificationPermissionIfNeeded()

        // Step 2: Ask battery optimization exemption
        requestBatteryOptimizationExemption()
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    // Already granted — nothing to do
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    // User previously denied — show rationale dialog then ask again
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Enable Notifications")
                        .setMessage("UniFlow needs notification permission to alert you 10 minutes before each class and for voice reminders.")
                        .setPositiveButton("Allow") { _, _ ->
                            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        .setNegativeButton("Not Now", null)
                        .show()
                }
                else -> {
                    // First time — ask directly
                    requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
        // Below Android 13 — POST_NOTIFICATIONS not needed, already allowed by default
    }

    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = android.net.Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }
}