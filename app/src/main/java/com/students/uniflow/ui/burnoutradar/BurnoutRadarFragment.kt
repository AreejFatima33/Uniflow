package com.students.uniflow.ui.burnoutradar

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.students.uniflow.R
import kotlinx.coroutines.launch

class BurnoutRadarFragment : Fragment() {

    private val viewModel: BurnoutRadarViewModel by viewModels()

    // Views
    private lateinit var btnAnalyze: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var layoutResult: com.google.android.material.card.MaterialCardView
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

                            // Risk level with color coding
                            tvRiskLevel.text = "Risk Level: ${result.riskLevel}"
                            tvRiskLevel.setTextColor(
                                when (result.riskLevel) {
                                    "High"   -> android.graphics.Color.parseColor("#D32F2F")
                                    "Medium" -> android.graphics.Color.parseColor("#F57C00")
                                    else     -> android.graphics.Color.parseColor("#388E3C")
                                }
                            )

                            tvSummary.text = result.summary
                            tvSuggestions.text = result.suggestions
                                .mapIndexed { i, s -> "${i + 1}. $s" }
                                .joinToString("\n")
                            tvEncouragement.text = "💙 ${result.encouragement}"
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
}