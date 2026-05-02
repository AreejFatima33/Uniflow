package com.students.uniflow.ui.deadlinegenie

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
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

        // Read study plan from Bundle
        val json = arguments?.getString("study_plan_json") ?: return
        val plan = Gson().fromJson(json, StudyPlanResult::class.java) ?: return

        binding.tvPlanTitle.text = "📅 ${plan.planTitle}"
        binding.tvTotalDays.text = "Total: ${plan.totalDays} days of study"

        // Build a card for each day
        plan.dailyPlan.forEach { dayPlan ->
            val card = buildDayCard(dayPlan.day, dayPlan.date, dayPlan.topic, dayPlan.tasks)
            binding.containerPlan.addView(card)
        }
    }

    private fun buildDayCard(
        dayNumber: Int,
        date: String,
        topic: String,
        tasks: List<String>
    ): CardView {
        val context = requireContext()

        // Alternate card colors
        val bgColor = if (dayNumber % 2 == 0)
            Color.parseColor("#E3F2FD")   // light blue
        else
            Color.parseColor("#F3E5F5")   // light purple

        val card = CardView(context).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(bgColor)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 0, 16)
            layoutParams = params
        }

        val inner = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 24, 32, 24)
        }

        // Day header
        inner.addView(TextView(context).apply {
            text = "Day $dayNumber — $date"
            textSize = 15f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A237E"))
        })

        // Topic
        inner.addView(TextView(context).apply {
            text = "📖 $topic"
            textSize = 14f
            setTextColor(Color.parseColor("#333333"))
            setPadding(0, 8, 0, 8)
        })

        // Tasks
        tasks.forEach { task ->
            inner.addView(TextView(context).apply {
                text = "✅ $task"
                textSize = 13f
                setTextColor(Color.parseColor("#555555"))
                setPadding(8, 2, 0, 2)
            })
        }

        card.addView(inner)
        return card
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}