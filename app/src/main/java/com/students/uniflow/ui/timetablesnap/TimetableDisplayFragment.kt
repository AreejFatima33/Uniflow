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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_timetable_display, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val container = view.findViewById<LinearLayout>(R.id.schedule_container)
        val tvEmpty   = view.findViewById<TextView>(R.id.tv_empty)

        // Read entries passed from TimetableSnapFragment
        val json = arguments?.getString("timetable_json")
        if (json.isNullOrEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        val type = object : TypeToken<List<TimetableEntry>>() {}.type
        val entries: List<TimetableEntry> = Gson().fromJson(json, type)

        if (entries.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            return
        }

        tvEmpty.visibility = View.GONE
        container.removeAllViews()

        // Group by day and display
        entries.groupBy { it.day }.forEach { (day, classes) ->
            val dayHeader = TextView(requireContext()).apply {
                text = "📅 $day"
                textSize = 17f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 28, 0, 8)
                setTextColor(android.graphics.Color.parseColor("#1565C0"))
            }
            container.addView(dayHeader)

            classes.sortedBy { it.time }.forEach { entry ->
                val row = TextView(requireContext()).apply {
                    text = "  🕐 ${entry.time}  |  📚 ${entry.subject}" +
                            "${if (entry.room.isNotEmpty()) "  |  🏠 ${entry.room}" else ""}" +
                            "${if (entry.professor.isNotEmpty()) "\n     👩‍🏫 ${entry.professor}" else ""}"
                    textSize = 14f
                    setPadding(16, 6, 0, 6)
                }
                container.addView(row)

                // Add divider
                val divider = View(requireContext()).apply {
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1)
                    setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
                }
                container.addView(divider)
            }
        }
    }
}