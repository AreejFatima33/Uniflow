package com.students.uniflow.ui.examoracle

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.databinding.FragmentExamOracleBinding
import com.students.uniflow.utils.CameraHelper
import kotlinx.coroutines.launch
import androidx.lifecycle.repeatOnLifecycle

class ExamOracleFragment : Fragment(R.layout.fragment_exam_oracle) {

    private var _binding: FragmentExamOracleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExamOracleViewModel by viewModels()
    private lateinit var cameraHelper: CameraHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExamOracleBinding.bind(view)

        viewModel.init(requireContext())

        cameraHelper = CameraHelper(
            context = requireContext(),
            lifecycleOwner = this,
            previewView = binding.previewView,
            onPhotoCaptured = { uri ->
                viewModel.processImage(uri)
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
                else 0xFFFFFFFF.toInt()
            )
        }

        binding.btnCapture.setOnClickListener {
            val paperName = binding.etPaperName.text?.toString()?.trim() ?: ""
            viewModel.setPaperName(paperName)
            cameraHelper.takePhoto()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ExamOracleUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnCapture.isEnabled = false
                        }

                        is ExamOracleUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val currentDest = findNavController().currentDestination?.id
                            if (currentDest == R.id.nav_exam) {
                                val json = Gson().toJson(state.result)
                                // FIX: both putString calls inside apply block
                                val bundle = Bundle().apply {
                                    putString("exam_result_json", json)
                                    putString("paper_name", viewModel.getPaperName())
                                }
                                findNavController().navigate(
                                    R.id.action_examOracle_to_examResult,
                                    bundle
                                )
                            }
                        }

                        is ExamOracleUiState.Error -> {
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