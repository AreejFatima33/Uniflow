package com.students.uniflow.ui.examoracle

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.data.model.ExamOracleResult
import com.students.uniflow.databinding.FragmentExamResultBinding

class ExamResultFragment : Fragment(R.layout.fragment_exam_result) {

    private var _binding: FragmentExamResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExamResultBinding.bind(view)

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
                findNavController().navigate(R.id.nav_home)
            }
        }

        val json = arguments?.getString("exam_result_json") ?: return
        val result = Gson().fromJson(json, ExamOracleResult::class.java) ?: return

        val paperName = arguments?.getString("paper_name") ?: ""
        binding.tvSubjectName.text = if (paperName.isNotBlank())
            "Subject: $paperName" else "Predicted exam topics"

        result.predictedTopics.forEach { topic ->
            val card = buildTopicCard(topic.topic, topic.probability, topic.practiceQuestions)
            binding.containerTopics.addView(card)
        }
    }

    private fun buildTopicCard(
        topic: String,
        probability: String,
        questions: List<String>
    ): com.google.android.material.card.MaterialCardView {
        val ctx = requireContext()

        // Colors per probability
        val cardBg = when (probability) {
            "High"   -> android.graphics.Color.parseColor("#FDFAF7")
            "Medium" -> android.graphics.Color.parseColor("#FDFAF7")
            else     -> android.graphics.Color.parseColor("#FDFAF7")
        }
        val accentColor = when (probability) {
            "High"   -> android.graphics.Color.parseColor("#2D8A4E")
            "Medium" -> android.graphics.Color.parseColor("#C9A030")
            else     -> android.graphics.Color.parseColor("#C04020")
        }
        val strokeColor = when (probability) {
            "High"   -> android.graphics.Color.parseColor("#A8D5B5")
            "Medium" -> android.graphics.Color.parseColor("#E8D080")
            else     -> android.graphics.Color.parseColor("#EDA090")
        }
        val probBg = when (probability) {
            "High"   -> android.graphics.Color.parseColor("#E8F5E9")
            "Medium" -> android.graphics.Color.parseColor("#FFF8E1")
            else     -> android.graphics.Color.parseColor("#FDE8E0")
        }
        val probPercent = when (probability) {
            "High"   -> "82%"
            "Medium" -> "61%"
            else     -> "38%"
        }

        val card = com.google.android.material.card.MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 14 }
            radius = 46f
            cardElevation = 2f
            strokeWidth = 2
            setCardBackgroundColor(cardBg)
            this.strokeColor = strokeColor
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 40, 44, 40)
        }

        // ── Header row: topic name + probability pill ──
        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 14 }
        }

        val tvTopic = TextView(ctx).apply {
            text = topic
            textSize = 15f
            typeface = resources.getFont(R.font.playfair_display_bold)
            setTextColor(android.graphics.Color.parseColor("#2A1010"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        // Probability pill
        val tvProb = TextView(ctx).apply {
            text = probability
            textSize = 11f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(accentColor)
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(probBg)
                cornerRadius = 20f
            }
            setPadding(20, 8, 20, 8)
        }

        headerRow.addView(tvTopic)
        headerRow.addView(tvProb)
        inner.addView(headerRow)

        // ── Progress bar ──
        val trackBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(android.graphics.Color.parseColor("#F0E8E5"))
            cornerRadius = 4f
        }
        val fillBg = android.graphics.drawable.GradientDrawable().apply {
            setColor(accentColor)
            cornerRadius = 4f
        }
        val fillPct = when (probability) { "High" -> 0.82f; "Medium" -> 0.61f; else -> 0.38f }

        val track = android.widget.FrameLayout(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 8
            ).also { it.bottomMargin = 18 }
            background = trackBg
        }
        val fill = View(ctx).apply {
            background = fillBg
            layoutParams = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT, 8
            )
            // Width set after layout
            tag = fillPct
        }
        track.addView(fill)
        inner.addView(track)

        // Post-layout: set fill width proportionally
        track.post {
            val params = fill.layoutParams as android.widget.FrameLayout.LayoutParams
            params.width = (track.width * fillPct).toInt()
            fill.layoutParams = params
        }

        // ── Divider ──
        inner.addView(View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.bottomMargin = 14 }
            setBackgroundColor(android.graphics.Color.parseColor("#EDD8D4"))
        })

        // ── Practice questions label ──
        inner.addView(TextView(ctx).apply {
            text = "Practice Questions"
            textSize = 12f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#7A5050"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 10 }
        })

        // ── Questions ──
        questions.forEachIndexed { i, q ->
            val tvQuestion = TextView(ctx).apply {
                text = "${i + 1}. $q"
                textSize = 13f
                typeface = resources.getFont(R.font.inter_regular)
                setTextColor(android.graphics.Color.parseColor("#4A2A2A"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }
            tvQuestion.setLineSpacing(0f, 1.5f)
            inner.addView(tvQuestion)
        }

        card.addView(inner)
        return card
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}