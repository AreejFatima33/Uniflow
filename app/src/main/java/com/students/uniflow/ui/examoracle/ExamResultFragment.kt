package com.students.uniflow.ui.examoracle

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
import com.students.uniflow.data.model.ExamOracleResult
import com.students.uniflow.databinding.FragmentExamResultBinding

class ExamResultFragment : Fragment(R.layout.fragment_exam_result) {

    private var _binding: FragmentExamResultBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExamResultBinding.bind(view)

        // Read result from Bundle
        val json = arguments?.getString("exam_result_json") ?: return
        val result = Gson().fromJson(json, ExamOracleResult::class.java) ?: return
        // Show subject name if provided
        val paperName = arguments?.getString("paper_name") ?: ""
        val tvSubjectName = view.findViewById<TextView>(R.id.tvSubjectName)
        if (paperName.isNotBlank()) {
            tvSubjectName.text = "Subject: $paperName"
            tvSubjectName.visibility = View.VISIBLE
        } else {
            tvSubjectName.visibility = View.GONE
        }

        // Build UI cards for each topic
        result.predictedTopics.forEach { topic ->
            val card = buildTopicCard(topic.topic, topic.probability, topic.practiceQuestions)
            binding.containerTopics.addView(card)
        }
    }

    private fun buildTopicCard(
        topic: String,
        probability: String,
        questions: List<String>
    ): CardView {
        val context = requireContext()

        // Color by probability
        val probColor = when (probability) {
            "High" -> Color.parseColor("#C8E6C9")   // green tint
            "Medium" -> Color.parseColor("#FFF9C4") // yellow tint
            else -> Color.parseColor("#FFCCBC")      // orange tint
        }

        val card = CardView(context).apply {
            radius = 16f
            cardElevation = 4f
            setCardBackgroundColor(probColor)
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

        // Topic name
        inner.addView(TextView(context).apply {
            text = "📌 $topic"
            textSize = 16f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#1A237E"))
        })

        // Probability label
        inner.addView(TextView(context).apply {
            text = "Probability: $probability"
            textSize = 13f
            setTextColor(Color.parseColor("#555555"))
            setPadding(0, 4, 0, 12)
        })

        // Practice questions
        inner.addView(TextView(context).apply {
            text = "Practice Questions:"
            textSize = 13f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#333333"))
        })

        questions.forEachIndexed { i, q ->
            inner.addView(TextView(context).apply {
                text = "${i + 1}. $q"
                textSize = 13f
                setTextColor(Color.parseColor("#444444"))
                setPadding(0, 4, 0, 0)
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