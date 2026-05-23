package com.students.uniflow.ui.timetablesnap

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.students.uniflow.R
import com.students.uniflow.utils.CameraHelper
import kotlinx.coroutines.launch

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
            if (granted) {
                // Camera granted, now ensure exact alarm constraints are met
                checkExactAlarmPermissionAndStartCamera()
            } else {
                tvStatus.text = "Camera permission denied"
            }
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

        // Initial security framework check
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

                    viewLifecycleOwner.lifecycleScope.launch {
                        com.students.uniflow.data.repository.BurnoutRepository(requireContext())
                            .logStudySession("TimetableSnap", 10)
                    }

                    val navOptions = androidx.navigation.NavOptions.Builder()
                        .setPopUpTo(R.id.nav_timetable, true)
                        .build()
                    findNavController().navigate(R.id.action_timetableSnap_to_timetableDisplay, bundle, navOptions)
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
            // Camera check pass -> forward execution stream to Exact Alarm verification
            checkExactAlarmPermissionAndStartCamera()
        } else {
            requestPermission.launch(Manifest.permission.CAMERA)
        }
    }

    /**
    // ADDED METHOD
     * Verifies system API level constraints for exact scheduling mechanisms.
     * Fires intent navigation to system settings if authorization parameters are absent.
     */
    private fun checkExactAlarmPermissionAndStartCamera() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!alarmManager.canScheduleExactAlarms()) {
                tvStatus.text = "Please enable Exact Alarms to allow class reminders."

                // EXPLICIT INTENT INJECTION FOR SYSTEM LEVEL SETTINGS
                val intent = Intent().apply {
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                    data = android.net.Uri.fromParts("package", requireContext().packageName, null)
                }
                startActivity(intent)
                return // Halt execution flow; camera tracking shouldn't activate until background tasks can safely register
            }
        }

        // Fallthrough if API level < 31 or if permission is already authorized
        startCamera()
    }

    private fun startCamera() {
        cameraHelper = CameraHelper(requireContext(), viewLifecycleOwner, previewView) { uri ->
            viewModel.processImage(uri)
        }
        cameraHelper.startCamera()
        tvStatus.text = "Point at your paper timetable and tap Capture"
    }

    override fun onDestroyView() {
        if (::cameraHelper.isInitialized) {
            cameraHelper.turnOffTorch()
        }
        super.onDestroyView()
    }
}