package com.students.uniflow.ui.burnoutradar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.card.MaterialCardView
import com.students.uniflow.R
import com.students.uniflow.worker.BurnoutWorker
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class BurnoutRadarFragment : Fragment() {

    private val viewModel: BurnoutRadarViewModel by viewModels()

    private lateinit var btnAnalyze: com.google.android.material.button.MaterialButton
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutResult: MaterialCardView
    private lateinit var tvRiskLevel: TextView
    private lateinit var tvSummary: TextView
    private lateinit var tvSuggestions: TextView
    private lateinit var tvEncouragement: TextView
    private lateinit var tvError: TextView
    private lateinit var tvIdle: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_burnout_radar, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarHeight = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<android.widget.FrameLayout>(R.id.header_frame)
                ?.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        // Back chevron → home
        view.post {
            view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
                findNavController().navigate(R.id.nav_home)
            }
        }

        // Schedule daily BurnoutWorker if not already running
        scheduleBurnoutWorker()

        btnAnalyze      = view.findViewById(R.id.btn_analyze_burnout)
        progressBar     = view.findViewById(R.id.progress_burnout)
        layoutResult    = view.findViewById(R.id.layout_burnout_result)
        tvRiskLevel     = view.findViewById(R.id.tv_risk_level)
        tvSummary       = view.findViewById(R.id.tv_burnout_summary)
        tvSuggestions   = view.findViewById(R.id.tv_suggestions)
        tvEncouragement = view.findViewById(R.id.tv_encouragement)
        tvError         = view.findViewById(R.id.tv_burnout_error)
        tvIdle          = view.findViewById(R.id.tv_burnout_idle)

        btnAnalyze.setOnClickListener {
            viewModel.analyzeBurnout()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is BurnoutUiState.Idle -> {
                            progressBar.visibility = View.GONE
                            layoutResult.visibility = View.GONE
                            tvError.visibility = View.GONE
                            tvIdle.visibility = View.VISIBLE
                        }
                        is BurnoutUiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            layoutResult.visibility = View.GONE
                            tvError.visibility = View.GONE
                            tvIdle.visibility = View.GONE
                        }
                        is BurnoutUiState.Success -> {
                            progressBar.visibility = View.GONE
                            tvError.visibility = View.GONE
                            tvIdle.visibility = View.GONE
                            layoutResult.visibility = View.VISIBLE

                            val result = state.result

                            // Risk level pill — color + background
                            val riskColor = when (result.riskLevel) {
                                "High"   -> android.graphics.Color.parseColor("#C04020")
                                "Medium" -> android.graphics.Color.parseColor("#C9A030")
                                else     -> android.graphics.Color.parseColor("#2D8A4E")
                            }
                            val riskBg = when (result.riskLevel) {
                                "High"   -> android.graphics.Color.parseColor("#FDE8E0")
                                "Medium" -> android.graphics.Color.parseColor("#FFF8E1")
                                else     -> android.graphics.Color.parseColor("#E8F5E9")
                            }
                            tvRiskLevel.text = result.riskLevel
                            tvRiskLevel.setTextColor(riskColor)
                            tvRiskLevel.background =
                                android.graphics.drawable.GradientDrawable().apply {
                                    setColor(riskBg)
                                    cornerRadius = 20f
                                }
                            tvRiskLevel.setPadding(36, 14, 36, 14)

                            tvSummary.text = result.summary
                            tvSuggestions.text = result.suggestions
                                .mapIndexed { i, s -> "${i + 1}. $s" }
                                .joinToString("\n")
                            tvEncouragement.text = "💙  ${result.encouragement}"
                        }
                        is BurnoutUiState.Error -> {
                            progressBar.visibility = View.GONE
                            layoutResult.visibility = View.GONE
                            tvIdle.visibility = View.GONE
                            tvError.visibility = View.VISIBLE
                            tvError.text = "Error: ${state.message}"
                        }
                    }
                }
            }
        }
    }

    // Schedules BurnoutWorker to run once every 24 hours
    // KEEP_EXISTING means it won't reschedule if already running
    private fun scheduleBurnoutWorker() {
        val workRequest = PeriodicWorkRequestBuilder<BurnoutWorker>(24, TimeUnit.HOURS)
            .build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "burnout_daily_check",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }
}