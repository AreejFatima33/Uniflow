package com.students.uniflow.ui.timetablesnap

import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.fragment.app.Fragment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.students.uniflow.R
import com.students.uniflow.data.model.TimetableEntry

class TimetableDisplayFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_timetable_display, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<LinearLayout>(R.id.schedule_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_empty)

        val json = arguments?.getString("timetable_json")
        if (json.isNullOrEmpty()) { tvEmpty.visibility = View.VISIBLE; return }

        val type = object : TypeToken<List<TimetableEntry>>() {}.type
        val entries: List<TimetableEntry> = Gson().fromJson(json, type)

        if (entries.isEmpty()) { tvEmpty.visibility = View.VISIBLE; return }

        tvEmpty.visibility = View.GONE
        container.removeAllViews()

        // Correct day order
        val dayOrder = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")

        val subjectColors = listOf(
            "#4A1010", "#6B1E1E", "#8B3A3A", "#5D1A2E", "#2C3E50",
            "#1A472A", "#4A235A", "#7D3C00", "#0B3954", "#3B1F5E"
        )
        val subjectColorMap = mutableMapOf<String, String>()
        var colorIndex = 0

        val grouped = entries.groupBy { it.day }

        // Sort by correct day order
        val sortedDays = dayOrder.filter { grouped.containsKey(it) }
        // Add any days not in dayOrder (fallback)
        val remainingDays = grouped.keys.filter { !dayOrder.contains(it) }

        (sortedDays + remainingDays).forEach { day ->
            val classes = grouped[day] ?: return@forEach

            // Day header card
            val dayCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also {
                    it.bottomMargin = 12
                    it.topMargin = 8
                }
                radius = 16f
                cardElevation = 0f
                setCardBackgroundColor(android.graphics.Color.parseColor("#3D0C0C"))
            }
            val dayText = TextView(requireContext()).apply {
                text = day.uppercase()
                textSize = 13f
                typeface = resources.getFont(R.font.inter_medium)
                setTextColor(android.graphics.Color.parseColor("#F5EDE8"))
                letterSpacing = 0.12f
                setPadding(48, 28, 48, 28)
            }
            dayCard.addView(dayText)
            container.addView(dayCard)

            // Class cards
            classes.sortedBy { it.time }.forEach { entry ->
                // Assign color per subject
                val color = subjectColorMap.getOrPut(entry.subject) {
                    subjectColors[colorIndex % subjectColors.size].also { colorIndex++ }
                }

                val classCard = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).also {
                        it.bottomMargin = 10
                        it.marginStart = 12
                    }
                    radius = 16f
                    cardElevation = 2f
                    setCardBackgroundColor(android.graphics.Color.parseColor("#FDFAF7"))
                    strokeWidth = 2
                    strokeColor = android.graphics.Color.parseColor(color)
                }

                val inner = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(0, 0, 0, 0)
                }

                // Left color strip
                val strip = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(6, 200)
                    minimumHeight = 200
                    setBackgroundColor(android.graphics.Color.parseColor(color))
                }

                val content = LinearLayout(requireContext()).apply {
                    orientation = LinearLayout.VERTICAL
                    setPadding(32, 20, 32, 20)
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                }

                // Time badge
                val timeBadge = TextView(requireContext()).apply {
                    text = "⏰  ${entry.time}"
                    textSize = 12f
                    typeface = resources.getFont(R.font.inter_medium)
                    setTextColor(android.graphics.Color.parseColor("#8B3A3A"))
                    setPadding(0, 0, 0, 8)
                }

                // Subject
                val subjectText = TextView(requireContext()).apply {
                    text = entry.subject
                    textSize = 16f
                    typeface = resources.getFont(R.font.playfair_display_bold)
                    setTextColor(android.graphics.Color.parseColor("#2C1810"))
                    setPadding(0, 0, 0, 4)
                }

                content.addView(timeBadge)
                content.addView(subjectText)

                if (entry.professor.isNotEmpty()) {
                    val profText = TextView(requireContext()).apply {
                        text = "👩‍🏫 ${entry.professor}"
                        textSize = 12f
                        typeface = resources.getFont(R.font.inter_regular)
                        setTextColor(android.graphics.Color.parseColor("#6B5448"))
                    }
                    content.addView(profText)
                }

                if (entry.room.isNotEmpty()) {
                    val roomText = TextView(requireContext()).apply {
                        text = "📍 Room ${entry.room}"
                        textSize = 11f
                        typeface = resources.getFont(R.font.inter_regular)
                        setTextColor(android.graphics.Color.parseColor("#A08070"))
                        setPadding(0, 2, 0, 0)
                    }
                    content.addView(roomText)
                }

                inner.addView(strip)
                inner.addView(content)
                classCard.addView(inner)
                container.addView(classCard)
            }
        }
    }
}