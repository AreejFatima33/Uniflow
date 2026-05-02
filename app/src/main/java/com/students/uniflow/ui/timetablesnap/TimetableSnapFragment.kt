package com.students.uniflow.ui.timetablesnap

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

class TimetableSnapFragment : Fragment() {

    private val viewModel: TimetableSnapViewModel by viewModels()
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
        return inflater.inflate(R.layout.fragment_timetable_snap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        previewView  = view.findViewById(R.id.preview_view)
        btnCapture   = view.findViewById(R.id.btn_capture)
        btnTorch     = view.findViewById(R.id.btn_torch)
        progressBar  = view.findViewById(R.id.progress_bar)
        tvStatus     = view.findViewById(R.id.tv_status)

        checkCameraPermission()

        btnCapture.setOnClickListener {
            tvStatus.text = "Reading timetable..."
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
                is TimetableUiState.Idle    -> { progressBar.visibility = View.GONE }
                is TimetableUiState.Loading -> {
                    progressBar.visibility = View.VISIBLE
                    tvStatus.text = "Parsing schedule with Gemini AI..."
                    btnCapture.isEnabled = false
                }
                is TimetableUiState.Success -> {
                    progressBar.visibility = View.GONE
                    btnCapture.isEnabled = true
                    tvStatus.text = "✅ ${state.entries.size} classes found! Reminders set."
                    val gson = com.google.gson.Gson()
                    val json = gson.toJson(state.entries)
                    val bundle = Bundle().apply { putString("timetable_json", json) }
                    findNavController().navigate(R.id.action_timetableSnap_to_timetableDisplay, bundle)
                }
                is TimetableUiState.Error -> {
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
        tvStatus.text = "Point at your paper timetable and tap Capture"
    }

    override fun onDestroyView() {
        cameraHelper.turnOffTorch()
        super.onDestroyView()
    }
}