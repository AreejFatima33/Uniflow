package com.students.uniflow.ui.history

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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

        val db = AppDatabase.getDatabase(requireContext())

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

            // ── LectureSnap Notes ────────────────────────────────────────────
            val notes = db.noteDao().getAll()
            if (notes.isEmpty()) {
                tvNotesEmpty.visibility = View.VISIBLE
            } else {
                tvNotesEmpty.visibility = View.GONE
                notes.forEach { note ->
                    layoutNotes.addView(
                        buildHistoryItem(
                            title = note.title,
                            subtitle = note.summary.take(80) + "...",
                            date = android.text.format.DateFormat
                                .format("dd MMM yyyy", note.savedAt).toString()
                        )
                    )
                }
            }

            // ── TimetableSnap ────────────────────────────────────────────────
            // uses 'timestamp' field, no savedAt
            val timetables = db.timetableDao().getAllEntriesList()
            if (timetables.isEmpty()) {
                tvTimetableEmpty.visibility = View.VISIBLE
            } else {
                tvTimetableEmpty.visibility = View.GONE
                timetables.forEach { t ->
                    layoutTimetable.addView(
                        buildHistoryItem(
                            title    = "${t.day} — ${t.subject}",
                            subtitle = "${t.time}  |  Room: ${t.room}  |  ${t.professor}",
                            date     = android.text.format.DateFormat
                                .format("dd MMM yyyy", t.timestamp).toString()
                        )
                    )
                }
            }

            // ── ExamOracle ───────────────────────────────────────────────────
            // uses 'createdAt' field, topic + probability
            val examTopics = db.examTopicDao().getAllTopics()
            if (examTopics.isEmpty()) {
                tvExamEmpty.visibility = View.VISIBLE
            } else {
                tvExamEmpty.visibility = View.GONE
                examTopics.forEach { topic ->
                    layoutExam.addView(
                        buildHistoryItem(
                            title    = topic.topic,
                            subtitle = "Probability: ${topic.probability}",
                            date     = android.text.format.DateFormat
                                .format("dd MMM yyyy", topic.createdAt).toString()
                        )
                    )
                }
            }

            // ── DeadlineGenie ────────────────────────────────────────────────
            // uses 'createdAt', fields: day, date, topic
            val plans = db.studyPlanDao().getAllPlans()
            if (plans.isEmpty()) {
                tvPlansEmpty.visibility = View.VISIBLE
            } else {
                tvPlansEmpty.visibility = View.GONE
                plans.forEach { plan ->
                    layoutPlans.addView(
                        buildHistoryItem(
                            title    = "Day ${plan.day}: ${plan.topic}",
                            subtitle = plan.date,
                            date     = android.text.format.DateFormat
                                .format("dd MMM yyyy", plan.createdAt).toString()
                        )
                    )
                }
            }

            // ── ConceptSnap ──────────────────────────────────────────────────
            // uses 'savedAt' field
            val concepts = db.conceptDao().getAll()
            if (concepts.isEmpty()) {
                tvConceptsEmpty.visibility = View.VISIBLE
            } else {
                tvConceptsEmpty.visibility = View.GONE
                concepts.forEach { concept ->
                    layoutConcepts.addView(
                        buildHistoryItem(
                            title    = concept.conceptTitle,
                            subtitle = concept.simpleExplanation.take(80) + "...",
                            date     = android.text.format.DateFormat
                                .format("dd MMM yyyy", concept.savedAt).toString()
                        )
                    )
                }
            }
        }
    }

    private fun buildHistoryItem(title: String, subtitle: String, date: String): LinearLayout {
        val card = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            setBackgroundColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 12) }
        }

        card.addView(TextView(requireContext()).apply {
            text = title
            textSize = 15f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFF1A1A1A.toInt())
        })

        card.addView(TextView(requireContext()).apply {
            text = subtitle
            textSize = 13f
            setTextColor(0xFF666666.toInt())
            setPadding(0, 4, 0, 0)
        })

        if (date.isNotBlank()) {
            card.addView(TextView(requireContext()).apply {
                text = date
                textSize = 11f
                setTextColor(0xFFAAAAAA.toInt())
                setPadding(0, 4, 0, 0)
            })
        }

        return card
    }
}