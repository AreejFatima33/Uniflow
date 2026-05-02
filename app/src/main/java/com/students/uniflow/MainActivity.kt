package com.students.uniflow

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.worker.BurnoutWorker
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
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