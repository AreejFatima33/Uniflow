package com.students.uniflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.students.uniflow.R

class HomeFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<View>(R.id.card_lecture).setOnClickListener {
            findNavController().navigate(R.id.nav_lecture)
        }

        view.findViewById<View>(R.id.card_timetable).setOnClickListener {
            findNavController().navigate(R.id.nav_timetable)
        }

        view.findViewById<View>(R.id.card_exam).setOnClickListener {
            findNavController().navigate(R.id.nav_exam)
        }

        view.findViewById<View>(R.id.card_deadline).setOnClickListener {
            findNavController().navigate(R.id.nav_deadline)
        }

        view.findViewById<View>(R.id.card_burnout).setOnClickListener {
            findNavController().navigate(R.id.nav_burnout_radar)
        }

        view.findViewById<View>(R.id.card_concept).setOnClickListener {
            findNavController().navigate(R.id.nav_concept_snap)
        }

        view.findViewById<View>(R.id.card_history).setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }
    }
}