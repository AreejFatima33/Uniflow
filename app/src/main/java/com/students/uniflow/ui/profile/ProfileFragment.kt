package com.students.uniflow.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.students.uniflow.databinding.FragmentProfileBinding
import com.students.uniflow.R

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var selectedAvatar = "boy"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load saved data
        val prefs = requireContext().getSharedPreferences("uniflow_profile", 0)
        val savedName = prefs.getString("user_name", "") ?: ""
        val savedAvatar = prefs.getString("avatar_type", "boy") ?: "boy"

        selectedAvatar = savedAvatar
        binding.etName.setText(savedName)
        updateAvatarSelection(savedAvatar)

        // Avatar selection
        binding.cardAvatarBoy.setOnClickListener {
            selectedAvatar = "boy"
            updateAvatarSelection("boy")
        }

        binding.cardAvatarGirl.setOnClickListener {
            selectedAvatar = "girl"
            updateAvatarSelection("girl")
        }

        // Save button
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            if (name.isEmpty()) {
                binding.tilName.error = "Please enter your name"
                return@setOnClickListener
            }
            binding.tilName.error = null

            prefs.edit()
                .putString("user_name", name)
                .putString("avatar_type", selectedAvatar)
                .apply()

            Toast.makeText(requireContext(), "Profile saved!", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        binding.etName.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val n = s.toString().trim()
                binding.tvAvatarLabel.text = if (n.isNotEmpty()) n else selectedAvatar
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Back button
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun updateAvatarSelection(type: String) {
        val name = binding.etName.text.toString().trim()
        val displayName = if (name.isNotEmpty()) name else if (type == "boy") "Boy" else "Girl"

        if (type == "boy") {
            binding.ivAvatarBoy.alpha = 1.0f
            binding.ivAvatarGirl.alpha = 0.4f
            binding.cardAvatarBoy.strokeWidth = 3
            binding.cardAvatarBoy.strokeColor = resources.getColor(R.color.maroon_primary, null)
            binding.cardAvatarGirl.strokeWidth = 1
            binding.cardAvatarGirl.strokeColor = resources.getColor(R.color.border_light, null)
            binding.ivMainAvatar.setImageResource(R.drawable.avatar_boy)
        } else {
            binding.ivAvatarGirl.alpha = 1.0f
            binding.ivAvatarBoy.alpha = 0.4f
            binding.cardAvatarGirl.strokeWidth = 3
            binding.cardAvatarGirl.strokeColor = resources.getColor(R.color.maroon_primary, null)
            binding.cardAvatarBoy.strokeWidth = 1
            binding.cardAvatarBoy.strokeColor = resources.getColor(R.color.border_light, null)
            binding.ivMainAvatar.setImageResource(R.drawable.avatar_girl)
        }
        binding.tvAvatarLabel.text = displayName
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}