package com.students.uniflow.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.card.MaterialCardView
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.R
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_history, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val db = AppDatabase.getInstance(requireContext())

        val layoutNotes     = view.findViewById<LinearLayout>(R.id.layout_notes_list)
        val layoutTimetable = view.findViewById<LinearLayout>(R.id.layout_timetable_list)
        val layoutExam      = view.findViewById<LinearLayout>(R.id.layout_exam_list)
        val layoutPlans     = view.findViewById<LinearLayout>(R.id.layout_plans_list)
        val layoutConcepts  = view.findViewById<LinearLayout>(R.id.layout_concepts_list)

        val tvNotesEmpty     = view.findViewById<TextView>(R.id.tv_notes_empty)
        val tvTimetableEmpty = view.findViewById<TextView>(R.id.tv_timetable_empty)
        val tvExamEmpty      = view.findViewById<TextView>(R.id.tv_exam_empty)
        val tvPlansEmpty     = view.findViewById<TextView>(R.id.tv_plans_empty)
        val tvConceptsEmpty  = view.findViewById<TextView>(R.id.tv_concepts_empty)

        viewLifecycleOwner.lifecycleScope.launch {

            val notes = db.noteDao().getAll()
            if (notes.isEmpty()) {
                tvNotesEmpty.visibility = View.VISIBLE
            } else {
                tvNotesEmpty.visibility = View.GONE
                notes.forEach { note ->
                    layoutNotes.addView(buildHistoryCard(
                        title = note.title,
                        subtitle = note.summary.take(80) + "...",
                        date = android.text.format.DateFormat
                            .format("dd MMM yyyy", note.savedAt).toString()
                    ))
                }
            }

            val timetables = db.timetableDao().getAllEntriesList()
            if (timetables.isEmpty()) {
                tvTimetableEmpty.visibility = View.VISIBLE
            } else {
                tvTimetableEmpty.visibility = View.GONE
                timetables.forEach { t ->
                    layoutTimetable.addView(buildHistoryCard(
                        title    = "${t.day} — ${t.subject}",
                        subtitle = "${t.time}  |  Room: ${t.room}  |  ${t.professor}",
                        date     = android.text.format.DateFormat
                            .format("dd MMM yyyy", t.timestamp).toString()
                    ))
                }
            }

            val examTopics = db.examTopicDao().getAllTopics()
            if (examTopics.isEmpty()) {
                tvExamEmpty.visibility = View.VISIBLE
            } else {
                tvExamEmpty.visibility = View.GONE
                examTopics.forEach { topic ->
                    layoutExam.addView(buildHistoryCard(
                        title    = topic.topic,
                        subtitle = "Probability: ${topic.probability}",
                        date     = android.text.format.DateFormat
                            .format("dd MMM yyyy", topic.createdAt).toString()
                    ))
                }
            }

            val plans = db.studyPlanDao().getAllPlans()
            if (plans.isEmpty()) {
                tvPlansEmpty.visibility = View.VISIBLE
            } else {
                tvPlansEmpty.visibility = View.GONE
                plans.forEach { plan ->
                    layoutPlans.addView(buildHistoryCard(
                        title    = "Day ${plan.day}: ${plan.topic}",
                        subtitle = plan.date,
                        date     = android.text.format.DateFormat
                            .format("dd MMM yyyy", plan.createdAt).toString()
                    ))
                }
            }

            val concepts = db.conceptDao().getAll()
            if (concepts.isEmpty()) {
                tvConceptsEmpty.visibility = View.VISIBLE
            } else {
                tvConceptsEmpty.visibility = View.GONE
                concepts.forEach { concept ->
                    layoutConcepts.addView(buildHistoryCard(
                        title    = concept.conceptTitle,
                        subtitle = concept.simpleExplanation.take(80) + "...",
                        date     = android.text.format.DateFormat
                            .format("dd MMM yyyy", concept.savedAt).toString()
                    ))
                }
            }
        }
    }

    private fun buildHistoryCard(title: String, subtitle: String, date: String): MaterialCardView {
        val ctx = requireContext()

        val card = MaterialCardView(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 10 }
            radius = resources.getDimension(com.intuit.sdp.R.dimen._18sdp).let { 46f }
            radius = 46f
            cardElevation = 4f
            strokeWidth = 2
            setCardBackgroundColor(resources.getColor(R.color.cream_card, null))
            strokeColor = resources.getColor(R.color.border_light, null)
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(44, 40, 44, 40)
        }

        // Left accent bar + title row
        val titleRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val accent = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(4, LinearLayout.LayoutParams.MATCH_PARENT)
                .also { it.marginEnd = 12 }
            background = android.graphics.drawable.GradientDrawable().apply {
                setColor(resources.getColor(R.color.maroon_dark, null))
                cornerRadius = 4f
            }
            minimumHeight = 48
        }

        val tvTitle = TextView(ctx).apply {
            text = title
            textSize = 14f
            typeface = resources.getFont(R.font.playfair_display_bold)
            setTextColor(resources.getColor(R.color.text_primary, null))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        titleRow.addView(accent)
        titleRow.addView(tvTitle)
        inner.addView(titleRow)

        inner.addView(TextView(ctx).apply {
            text = subtitle
            textSize = 12f
            typeface = resources.getFont(R.font.inter_regular)
            setTextColor(resources.getColor(R.color.text_hint, null))
            setPadding(16, 6, 0, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        })

        if (date.isNotBlank()) {
            inner.addView(TextView(ctx).apply {
                text = date
                textSize = 10f
                typeface = resources.getFont(R.font.inter_regular)
                setTextColor(resources.getColor(R.color.text_hint, null))
                setPadding(16, 4, 0, 0)
                alpha = 0.7f
            })
        }

        card.addView(inner)
        return card
    }
}