package com.students.uniflow.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
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

        // Load saved profile
        val prefs = requireContext().getSharedPreferences("uniflow_profile", 0)
        val name = prefs.getString("user_name", "Student") ?: "Student"
        val avatarType = prefs.getString("avatar_type", "boy") ?: "boy"

        view.findViewById<TextView>(R.id.tv_username_header).text = name
        view.findViewById<ImageView>(R.id.iv_avatar_small).setImageResource(
            if (avatarType == "girl") R.drawable.avatar_girl else R.drawable.avatar_boy
        )

        // Profile button → navigate to profile screen
        view.findViewById<View>(R.id.btn_profile).setOnClickListener {
            findNavController().navigate(R.id.nav_profile)
        }

        // Feature cards

        view.findViewById<View>(R.id.card_grade_predictor_home).setOnClickListener {
            findNavController().navigate(R.id.nav_grade_predictor)
        }
        view.findViewById<View>(R.id.card_scan_notes_home).setOnClickListener {
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

        view.findViewById<View>(R.id.card_voice).setOnClickListener {
            findNavController().navigate(R.id.nav_voice_reminder)
        }
        view.findViewById<View>(R.id.card_history).setOnClickListener {
            findNavController().navigate(R.id.nav_history)
        }
    }
}