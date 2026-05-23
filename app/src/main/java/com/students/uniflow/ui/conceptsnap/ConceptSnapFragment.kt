package com.students.uniflow.ui.conceptsnap

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.google.gson.Gson
import com.students.uniflow.R
import com.students.uniflow.utils.CameraHelper
import kotlinx.coroutines.launch

class ConceptSnapFragment : Fragment() {

    private val viewModel: ConceptSnapViewModel by viewModels()

    private var cameraHelper: CameraHelper? = null
    private var cameraStarted = false
    private var capturedImageUri: Uri? = null

    private lateinit var btnCapture: Button
    private lateinit var btnTorch: ImageButton
    private lateinit var etQuestion: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView
    private lateinit var cameraPreview: PreviewView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_concept_snap, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        btnCapture   = view.findViewById(R.id.btn_capture_concept)
        btnTorch     = view.findViewById(R.id.btn_torch_concept)
        etQuestion   = view.findViewById(R.id.et_concept_question)
        progressBar  = view.findViewById(R.id.progress_concept)
        tvStatus     = view.findViewById(R.id.tv_concept_status)
        cameraPreview = view.findViewById(R.id.camera_preview_concept)

        // Initialize CameraHelper — onPhotoCaptured fires after takePhoto()
        cameraHelper = CameraHelper(
            context = requireContext(),
            lifecycleOwner = viewLifecycleOwner,
            previewView = cameraPreview,
            onPhotoCaptured = { uri ->
                capturedImageUri = uri
                tvStatus.text = "Image captured! Sending to Gemini Vision..."
                cameraHelper?.turnOffTorch()
                val question = etQuestion.text.toString().trim()
                viewModel.explainConcept(uri, question)
            }
        )

        // Start camera immediately when fragment opens
        cameraHelper?.startCamera()
        cameraStarted = true

        // Single button: first tap = take photo, subsequent taps = retake
        btnCapture.setOnClickListener {
            if (!cameraStarted) {
                cameraHelper?.startCamera()
                cameraStarted = true
            }
            cameraHelper?.takePhoto()
            tvStatus.text = "Capturing..."
        }

        // Torch toggle
        btnTorch.setOnClickListener {
            val isOn = cameraHelper?.toggleTorch() ?: false
            btnTorch.alpha = if (isOn) 1.0f else 0.4f
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is ConceptUiState.Idle -> {
                            progressBar.visibility = View.GONE
                            btnCapture.isEnabled = true
                        }
                        is ConceptUiState.Loading -> {
                            progressBar.visibility = View.VISIBLE
                            btnCapture.isEnabled = false
                            tvStatus.text = "Sending image to Gemini Vision..."
                        }
                        is ConceptUiState.Success -> {
                            progressBar.visibility = View.GONE
                            btnCapture.isEnabled = true

                            viewLifecycleOwner.lifecycleScope.launch {
                                com.students.uniflow.data.repository.BurnoutRepository(requireContext())
                                    .logStudySession("ConceptSnap", 10)
                            }
                            val currentDest = findNavController().currentDestination?.id
                            if (currentDest == R.id.nav_concept_snap) {
                                val json = Gson().toJson(state.result)
                                val bundle = Bundle().apply { putString("concept_json", json) }
                                findNavController().navigate(
                                    R.id.action_conceptSnap_to_conceptResult, bundle
                                )
                                viewModel.reset()
                            }
                        }
                        is ConceptUiState.Error -> {
                            progressBar.visibility = View.GONE
                            btnCapture.isEnabled = true
                            tvStatus.text = "Error: ${state.message}"
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraHelper = null
        cameraStarted = false
    }
}