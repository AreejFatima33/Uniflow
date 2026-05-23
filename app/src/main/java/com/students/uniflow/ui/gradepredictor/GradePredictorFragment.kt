package com.students.uniflow.ui.gradepredictor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.Lifecycle
import com.students.uniflow.R
import com.students.uniflow.databinding.FragmentGradePredictorBinding
import kotlinx.coroutines.launch

class GradePredictorFragment : Fragment() {

    private var _binding: FragmentGradePredictorBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GradePredictorViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGradePredictorBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Status bar insets fix
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarHeight = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<android.widget.FrameLayout>(R.id.header_frame)
                ?.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

// Back chevron
        view.post {
            view.findViewById<android.view.View>(R.id.btn_back)?.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        binding.btnCalculate.setOnClickListener {
            val subject = binding.etSubject.text.toString().trim().ifBlank { "Subject" }
            val quizMarks = binding.etQuizMarks.text.toString().toDoubleOrNull() ?: 0.0
            val quizTotal = binding.etQuizTotal.text.toString().toDoubleOrNull()
            val assignMarks = binding.etAssignmentMarks.text.toString().toDoubleOrNull() ?: 0.0
            val assignTotal = binding.etAssignmentTotal.text.toString().toDoubleOrNull()
            val midMarks = binding.etMidtermMarks.text.toString().toDoubleOrNull() ?: 0.0
            val midTotal = binding.etMidtermTotal.text.toString().toDoubleOrNull()

            if (quizTotal == null || assignTotal == null || midTotal == null) {
                Toast.makeText(requireContext(), "Please fill in all Total fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.calculateGrade(
                subject,
                quizMarks, quizTotal,
                assignMarks, assignTotal,
                midMarks, midTotal
            )
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is GradePredictorUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.resultCard.visibility = View.GONE
                        }
                        is GradePredictorUiState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.resultCard.visibility = View.GONE
                        }
                        is GradePredictorUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.resultCard.visibility = View.VISIBLE

                                viewLifecycleOwner.lifecycleScope.launch {
                                    com.students.uniflow.data.repository.BurnoutRepository(requireContext())
                                        .logStudySession("GradePredictor", 10)
                                }

                            val r = state.result
                            binding.tvCurrentGrade.text = r.grade
                            binding.tvCurrentScore.text =
                                "Current Score (before final): ${String.format("%.1f", r.currentPercentage)}%"

                            binding.tvRequiredForA.text = when {
                                r.requiredForA < 0 -> "Grade A: Impossible (even 100% won't be enough)"
                                r.requiredForA == 0.0 -> "Grade A: Already secured ✅"
                                else -> "Grade A: Need ${String.format("%.1f", r.requiredForA)}% in final"
                            }
                            binding.tvRequiredForB.text = when {
                                r.requiredForB < 0 -> "Grade B: Impossible"
                                r.requiredForB == 0.0 -> "Grade B: Already secured ✅"
                                else -> "Grade B: Need ${String.format("%.1f", r.requiredForB)}% in final"
                            }
                            binding.tvRequiredForC.text = when {
                                r.requiredForC < 0 -> "Grade C: Impossible"
                                r.requiredForC == 0.0 -> "Grade C: Already secured ✅"
                                else -> "Grade C: Need ${String.format("%.1f", r.requiredForC)}% in final"
                            }
                        }
                        is GradePredictorUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}