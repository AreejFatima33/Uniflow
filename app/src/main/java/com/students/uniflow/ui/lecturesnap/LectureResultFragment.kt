package com.students.uniflow.ui.lecturesnap

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.students.uniflow.R

class LectureResultFragment : Fragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_lecture_result, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val title     = arguments?.getString("title")    ?: "Lecture Notes"
        val summary   = arguments?.getString("summary")   ?: ""
        val keyPoints = arguments?.getStringArrayList("keyPoints") ?: arrayListOf()

        view.findViewById<TextView>(R.id.tv_title).text   = title
        view.findViewById<TextView>(R.id.tv_summary).text = summary
        view.findViewById<TextView>(R.id.tv_key_points).text =
            keyPoints.mapIndexed { i, p -> "${i + 1}. $p" }.joinToString("\n\n")
    }
}