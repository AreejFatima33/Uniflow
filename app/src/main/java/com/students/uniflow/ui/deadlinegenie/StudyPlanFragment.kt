package com.students.uniflow.ui.deadlinegenie

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.data.model.StudyPlanResult
import com.students.uniflow.databinding.FragmentStudyPlanBinding

class StudyPlanFragment : Fragment(R.layout.fragment_study_plan) {

    private var _binding: FragmentStudyPlanBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStudyPlanBinding.bind(view)

        // Status bar insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarHeight = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<android.widget.FrameLayout>(R.id.header_frame)
                ?.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        // Back chevron
        view.post {
            view.findViewById<android.view.View>(R.id.btn_back)?.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        val json = arguments?.getString("study_plan_json") ?: return
        val plan = Gson().fromJson(json, StudyPlanResult::class.java) ?: return

        binding.tvPlanTitle.text = plan.planTitle
        binding.tvTotalDays.text = "${plan.totalDays} days of focused study"

        plan.dailyPlan.forEach { dayPlan ->
            buildDayCard(dayPlan.day, dayPlan.date, dayPlan.topic, dayPlan.tasks)
        }
    }

    private fun buildDayCard(dayNumber: Int, date: String, topic: String, tasks: List<String>) {
        val ctx = requireContext()

        val card = com.google.android.material.card.MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 12 }
            radius = 46f
            cardElevation = 2f
            strokeWidth = 2
            setCardBackgroundColor(android.graphics.Color.parseColor("#FDFAF7"))
            strokeColor = android.graphics.Color.parseColor("#EDD8D4")
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 36, 44, 36)
        }

        // Day number pill
        val tvPill = TextView(ctx).apply {
            text = "DAY $dayNumber"
            textSize = 9f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#B06060"))
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(android.graphics.Color.parseColor("#FDE8E0"))
                cornerRadius = 20f
            }
            setPadding(20, 8, 20, 8)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 10 }
        }

        // Date
        val tvDate = TextView(ctx).apply {
            text = "📅  $date"
            textSize = 12f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#7A5050"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }

        // Topic
        val tvTopic = TextView(ctx).apply {
            text = topic
            textSize = 15f
            typeface = resources.getFont(R.font.playfair_display_bold)
            setTextColor(android.graphics.Color.parseColor("#2A1010"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 12 }
        }

        // Divider
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.bottomMargin = 12 }
            setBackgroundColor(android.graphics.Color.parseColor("#EDD8D4"))
        }

        inner.addView(tvPill)
        inner.addView(tvDate)
        inner.addView(tvTopic)
        inner.addView(divider)

        // Tasks
        tasks.forEach { task ->
            val tvTask = TextView(ctx).apply {
                text = "✓  $task"
                textSize = 13f
                typeface = resources.getFont(R.font.inter_regular)
                setTextColor(android.graphics.Color.parseColor("#5A3A3A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 6 }
            }
            inner.addView(tvTask)
        }

        card.addView(inner)
        binding.containerPlan.addView(card)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}