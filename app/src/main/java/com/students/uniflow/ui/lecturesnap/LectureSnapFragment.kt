package com.students.uniflow.ui.lecturesnap

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.students.uniflow.R
import com.students.uniflow.utils.CameraHelper

class LectureSnapFragment : Fragment() {

    private val viewModel: LectureSnapViewModel by viewModels()
    private lateinit var cameraHelper: CameraHelper
    private lateinit var previewView: PreviewView
    private lateinit var btnCapture: Button
    private lateinit var btnTorch: ImageButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvStatus: TextView

    private val requestPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera() else tvStatus.text = "Camera permission denied"
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.fragment_lecture_snap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        previewView  = view.findViewById(R.id.preview_view)
        btnCapture   = view.findViewById(R.id.btn_capture)
        btnTorch     = view.findViewById(R.id.btn_torch)
        progressBar  = view.findViewById(R.id.progress_bar)
        tvStatus     = view.findViewById(R.id.tv_status)

        checkCameraPermission()

        btnCapture.setOnClickListener {
            tvStatus.text = "Reading text from photo..."
            cameraHelper.takePhoto()
        }

        btnTorch.setOnClickListener {
            val isOn = cameraHelper.toggleTorch()
            btnTorch.setImageResource(
                if (isOn) android.R.drawable.btn_star_big_on
                else android.R.drawable.btn_star_big_off
            )
            btnTorch.setColorFilter(
                if (isOn) 0xFFFFFF00.toInt()
                else 0xFF888888.toInt()
            )
        }

        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LectureUiState.Idle    -> { progressBar.visibility = View.GONE }
                is LectureUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvStatus.text = "Analyzing with Gemini AI..."
                    btnCapture.isEnabled = false
                }
                is LectureUiState.Success -> {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    val bundle = Bundle().apply {
                        putString("title",    state.result.title)
                        putString("summary",   state.result.summary)
                        putStringArrayList("keyPoints", ArrayList(state.result.keyPoints))
                    }
                    findNavController().navigate(R.id.action_lectureSnap_to_lectureResult, bundle)
                }
                is LectureUiState.Error -> {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    tvStatus.text = "Error: ${state.message}"
                }
            }
        }
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun startCamera() {
        cameraHelper = CameraHelper(requireContext(), viewLifecycleOwner, previewView) { uri ->
            viewModel.processImage(uri)
        }
        cameraHelper.startCamera()
        tvStatus.text = "Point at lecture notes and tap Capture"
    }

    override fun onDestroyView() {
        cameraHelper.turnOffTorch()
        super.onDestroyView()
    }
}