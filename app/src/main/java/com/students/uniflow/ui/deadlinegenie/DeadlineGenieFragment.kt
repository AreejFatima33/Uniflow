package com.students.uniflow.ui.deadlinegenie

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.databinding.FragmentDeadlineGenieBinding
import com.students.uniflow.utils.CameraHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle

class DeadlineGenieFragment : Fragment(R.layout.fragment_deadline_genie) {

    private var _binding: FragmentDeadlineGenieBinding? = null
    private val binding get() = _binding!!
    private val viewModel: DeadlineGenieViewModel by viewModels()
    private lateinit var cameraHelper: CameraHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDeadlineGenieBinding.bind(view)

        viewModel.init(requireContext())

        // Subject and exam date are read at capture time, not at init time
        // So we initialise CameraHelper with a placeholder and call processImage manually
        cameraHelper = CameraHelper(
            context = requireContext(),
            lifecycleOwner = this,
            previewView = binding.previewView,
            onPhotoCaptured = { uri ->
                val subject = binding.etSubjectName.text.toString().trim()
                val examDate = binding.etExamDate.text.toString().trim()
                if (subject.isEmpty() || examDate.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        "Please enter subject name and exam date first",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    viewModel.processSyllabus(uri, examDate, subject)
                }
            }
        )
        cameraHelper.startCamera()

        binding.btnTorch.setOnClickListener {
            val isOn = cameraHelper.toggleTorch()
            binding.btnTorch.setImageResource(
                if (isOn) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            binding.btnTorch.setColorFilter(
                if (isOn) 0xFFFFFF00.toInt()
                else 0xFF888888.toInt()
            )
        }

        binding.btnCapture.setOnClickListener {
            // Validate before taking photo
            val subject = binding.etSubjectName.text.toString().trim()
            val examDate = binding.etExamDate.text.toString().trim()
            if (subject.isEmpty() || examDate.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please enter subject name and exam date first",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }
            cameraHelper.takePhoto()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is DeadlineGenieUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnCapture.isEnabled = false
                        }

                        is DeadlineGenieUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val currentDest = findNavController().currentDestination?.id

                            // ADD THIS LINE TEMPORARILY
                            android.util.Log.d(
                                "UNIFLOW_NAV",
                                "Current dest: $currentDest, nav_deadline id: ${R.id.nav_deadline}"
                            )

                            if (currentDest == R.id.nav_deadline) {
                                val json = Gson().toJson(state.result)
                                val bundle = Bundle().apply { putString("study_plan_json", json) }
                                findNavController().navigate(
                                    R.id.action_deadlineGenie_to_studyPlan,
                                    bundle
                                )
                            } else {
                                android.util.Log.d(
                                    "UNIFLOW_NAV",
                                    "Navigation skipped — IDs don't match!"
                                )
                            }
                        }

                        is DeadlineGenieUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCapture.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG)
                                .show()
                        }

                        else -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCapture.isEnabled = true
                        }
                    }
                }
            }
        }
    }
    override fun onDestroyView() {
        cameraHelper.turnOffTorch()
        super.onDestroyView()
        _binding = null
    }
}
