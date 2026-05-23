package com.students.uniflow.ui.conceptsnap

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.data.model.ConceptResult

class ConceptResultFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_concept_result, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar insets
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarHeight = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<FrameLayout>(R.id.header_frame)
                ?.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        // Back chevron → home
        view.post {
            view.findViewById<View>(R.id.btn_back)?.setOnClickListener {
                findNavController().navigate(R.id.nav_home)
            }
        }

        val json = arguments?.getString("concept_json") ?: return
        val result = Gson().fromJson(json, ConceptResult::class.java)

        view.findViewById<TextView>(R.id.tv_concept_title).text = result.conceptTitle
        view.findViewById<TextView>(R.id.tv_simple_explanation).text = result.simpleExplanation
        view.findViewById<TextView>(R.id.tv_analogy).text = result.realLifeAnalogy
        view.findViewById<TextView>(R.id.tv_key_points).text = result.keyPoints
            .mapIndexed { i, p -> "${i + 1}. $p" }
            .joinToString("\n")
        view.findViewById<TextView>(R.id.tv_exam_tip).text = result.examTip
    }
}