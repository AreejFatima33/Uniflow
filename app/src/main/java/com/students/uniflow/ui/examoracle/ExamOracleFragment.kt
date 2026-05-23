package com.students.uniflow.ui.examoracle

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.databinding.FragmentExamOracleBinding
import com.students.uniflow.utils.CameraHelper
import kotlinx.coroutines.launch

class ExamOracleFragment : Fragment(R.layout.fragment_exam_oracle) {

    private var _binding: FragmentExamOracleBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ExamOracleViewModel by viewModels()
    private lateinit var cameraHelper: CameraHelper

    private var papersScanned = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentExamOracleBinding.bind(view)

        viewModel.init(requireContext())

        cameraHelper = CameraHelper(
            context = requireContext(),
            lifecycleOwner = this,
            previewView = binding.previewView,
            onPhotoCaptured = { uri ->
                viewModel.addPaperImage(uri)
                papersScanned++
                updateUiAfterCapture()
                Toast.makeText(
                    requireContext(),
                    "Paper $papersScanned captured ✓  Scan another or tap Analyze",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
        cameraHelper.startCamera()

        binding.btnTorch.setOnClickListener {
            val isOn = cameraHelper.toggleTorch()
            binding.btnTorch.setImageResource(
                if (isOn) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
        }

        // btnCapture — always the scan button, label changes after first scan
        binding.btnCapture.setOnClickListener {
            cameraHelper.takePhoto()
        }

        // btnAddPaper hidden after first scan — not used anymore
        binding.btnAddPaper.visibility = View.GONE

        // btnAnalyze — triggers AI analysis of all queued papers
        binding.btnAnalyze.setOnClickListener {
            val paperName = binding.etPaperName.text?.toString()?.trim() ?: ""
            viewModel.setPaperName(paperName)
            viewModel.setPapersCount(papersScanned)
            viewModel.analyzeAllPapers()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ExamOracleUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnCapture.isEnabled = false
                            binding.btnAnalyze.isEnabled = false
                            binding.btnCapture.text = "Scanning..."
                            binding.btnAnalyze.text = "Analyzing $papersScanned paper(s)..."
                        }
                        is ExamOracleUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            val currentDest = findNavController().currentDestination?.id
                            if (currentDest == R.id.nav_exam) {
                                // ADD THIS ↓
                                viewLifecycleOwner.lifecycleScope.launch {
                                    com.students.uniflow.data.repository.BurnoutRepository(requireContext())
                                        .logStudySession("ExamOracle", 20)
                                }
                                // ADD THIS ↑
                                val bundle = Bundle().apply {
                                    putString("exam_result_json", Gson().toJson(state.result))
                                    putString("paper_name", viewModel.getPaperName())
                                    putInt("papers_count", papersScanned)
                                }
                                findNavController().navigate(R.id.action_examOracle_to_examResult, bundle)
                            }
                        }
                        is ExamOracleUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCapture.isEnabled = true
                            binding.btnAnalyze.isEnabled = true
                            // Restore correct labels after error
                            binding.btnCapture.text = if (papersScanned == 0)
                                "Scan Paper" else "📷 Scan Another"
                            binding.btnAnalyze.text = "Analyze $papersScanned Paper(s)"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                        else -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnCapture.isEnabled = true
                            binding.btnAnalyze.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun updateUiAfterCapture() {
        // Show counter badge
        binding.layoutPaperCount.visibility = View.VISIBLE
        binding.tvPaperCount.text = papersScanned.toString()

        // btnCapture changes label — stays visible always
        binding.btnCapture.text = "📷 Scan Another"
        binding.btnCapture.isEnabled = true

        // btnAddPaper stays hidden — not needed
        binding.btnAddPaper.visibility = View.GONE

        // Show Analyze button after first paper
        binding.btnAnalyze.visibility = View.VISIBLE
        binding.btnAnalyze.text = "Analyze $papersScanned Paper(s)"
        binding.btnAnalyze.isEnabled = true
    }

    override fun onDestroyView() {
        cameraHelper.turnOffTorch()
        super.onDestroyView()
        _binding = null
    }
}