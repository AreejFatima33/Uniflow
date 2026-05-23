package com.students.uniflow.ui.lecturesnap

import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.students.uniflow.R
import com.students.uniflow.data.model.Flashcard
import com.students.uniflow.data.model.QuizQuestion

class LectureResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_lecture_result, container, false)

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

        // Back chevron
        view.findViewById<android.view.View>(R.id.btn_back)?.setOnClickListener {
            // Navigate specifically to the home destination and clear the backstack
            findNavController().navigate(R.id.nav_home, null, androidx.navigation.NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true) // Clears the entire graph stack
                .build())
        }

        val gson = Gson()
        val title     = arguments?.getString("title")    ?: "Lecture Notes"
        val summary   = arguments?.getString("summary")  ?: ""
        val keyPoints = arguments?.getStringArrayList("keyPoints") ?: arrayListOf()

        // Deserialize flashcards
        val flashcardsJson = arguments?.getString("flashcardsJson") ?: "[]"
        val flashcardType  = object : TypeToken<List<Flashcard>>() {}.type
        val flashcards: List<Flashcard> = try {
            gson.fromJson(flashcardsJson, flashcardType) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        // Deserialize quiz questions
        val quizJson  = arguments?.getString("quizJson") ?: "[]"
        val quizType  = object : TypeToken<List<QuizQuestion>>() {}.type
        val quizQuestions: List<QuizQuestion> = try {
            gson.fromJson(quizJson, quizType) ?: emptyList()
        } catch (e: Exception) { emptyList() }

        // Fill title + summary + key points
        view.findViewById<TextView>(R.id.tv_title).text   = title
        view.findViewById<TextView>(R.id.tv_summary).text = summary
        view.findViewById<TextView>(R.id.tv_key_points).text =
            keyPoints.mapIndexed { i, p -> "${i + 1}. $p" }.joinToString("\n\n")

        // Flashcards — show card only if data exists
        if (flashcards.isNotEmpty()) {
            view.findViewById<android.view.View>(R.id.card_flashcards).visibility = View.VISIBLE
            val flashcardsContainer = view.findViewById<LinearLayout>(R.id.flashcards_container)
            flashcards.forEachIndexed { i, card ->
                buildFlashcard(i + 1, card, flashcardsContainer)
            }
        }

        // Quiz — show card only if data exists
        if (quizQuestions.isNotEmpty()) {
            view.findViewById<android.view.View>(R.id.card_quiz).visibility = View.VISIBLE
            val quizContainer = view.findViewById<LinearLayout>(R.id.quiz_container)
            quizQuestions.forEachIndexed { i, q ->
                buildQuizQuestion(i + 1, q, quizContainer)
            }
        }
    }

    private fun buildFlashcard(num: Int, card: Flashcard, container: LinearLayout) {
        val ctx = requireContext()

        // Question row
        val tvQ = TextView(ctx).apply {
            text = "Q$num. ${card.question}"
            textSize = 14f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#F5F0EB"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 6 }
        }

        // Answer row — tap to reveal
        val tvA = TextView(ctx).apply {
            text = "Tap to reveal answer"
            textSize = 13f
            typeface = resources.getFont(R.font.inter_regular)
            setTextColor(android.graphics.Color.parseColor("#99F5F0EB"))
            setPadding(0, 0, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 20 }
            var revealed = false
            setOnClickListener {
                if (!revealed) {
                    text = "A: ${card.answer}"
                    setTextColor(android.graphics.Color.parseColor("#F5F0EB"))
                    revealed = true
                } else {
                    text = "Tap to reveal answer"
                    setTextColor(android.graphics.Color.parseColor("#99F5F0EB"))
                    revealed = false
                }
            }
        }

        // Divider
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.bottomMargin = 16 }
            setBackgroundColor(android.graphics.Color.parseColor("#44F5F0EB"))
        }

        container.addView(tvQ)
        container.addView(tvA)
        if (num < container.childCount) container.addView(divider)
    }

    private fun buildQuizQuestion(num: Int, q: QuizQuestion, container: LinearLayout) {
        val ctx = requireContext()

        val tvQ = TextView(ctx).apply {
            text = "Q$num. ${q.question}"
            textSize = 14f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#2A1010"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 8 }
        }
        container.addView(tvQ)

        // Options A/B/C/D
        q.options.forEachIndexed { oi, option ->
            val letter = listOf("A", "B", "C", "D").getOrElse(oi) { "${oi + 1}" }
            val tvOption = TextView(ctx).apply {
                text = "$letter.  $option"
                textSize = 13f
                typeface = resources.getFont(R.font.inter_regular)
                setTextColor(android.graphics.Color.parseColor("#5A3A3A"))
                setPadding(16, 0, 0, 0)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 4 }
            }
            container.addView(tvOption)
        }

        // Answer reveal
        val tvAnswer = TextView(ctx).apply {
            text = "Tap to see answer"
            textSize = 12f
            typeface = resources.getFont(R.font.inter_medium)
            setTextColor(android.graphics.Color.parseColor("#B06060"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also {
                it.topMargin = 8
                it.bottomMargin = 8
            }
            var revealed = false
            setOnClickListener {
                if (!revealed) {
                    text = "Answer: ${q.answer}"
                    setTextColor(android.graphics.Color.parseColor("#2D8A4E"))
                    revealed = true
                } else {
                    text = "Tap to see answer"
                    setTextColor(android.graphics.Color.parseColor("#B06060"))
                    revealed = false
                }
            }
        }
        container.addView(tvAnswer)

        // Divider
        val divider = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).also { it.topMargin = 8; it.bottomMargin = 16 }
            setBackgroundColor(android.graphics.Color.parseColor("#EDD8D4"))
        }
        container.addView(divider)
    }
}