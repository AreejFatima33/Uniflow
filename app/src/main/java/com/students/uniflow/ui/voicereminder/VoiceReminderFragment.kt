package com.students.uniflow.ui.voicereminder

import android.Manifest
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.button.MaterialButton
import com.students.uniflow.R
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.ReminderEntity
import com.students.uniflow.databinding.FragmentVoiceReminderBinding
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.utils.SpeechHelper
import kotlinx.coroutines.launch

class VoiceReminderFragment : Fragment() {

    private var _binding: FragmentVoiceReminderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VoiceReminderViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null

    private val remindersEntityList = mutableListOf<ReminderEntity>()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startListening()
        else Toast.makeText(requireContext(), "Microphone permission is required", Toast.LENGTH_LONG).show()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVoiceReminderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Back chevron — post ensures view is fully laid out before registering
        view.post {
            view.findViewById<android.view.View>(R.id.btn_back)?.setOnClickListener {
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        // Status bar insets fix
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            val statusBarHeight = insets.getInsets(
                androidx.core.view.WindowInsetsCompat.Type.statusBars()
            ).top
            view.findViewById<android.widget.FrameLayout>(R.id.header_frame)
                ?.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        loadSavedReminders()

        binding.btnMic.setOnClickListener {
            when {
                !SpeechHelper.isAvailable(requireContext()) ->
                    Toast.makeText(requireContext(), "Speech recognition not available", Toast.LENGTH_LONG).show()
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.RECORD_AUDIO)
                        == PackageManager.PERMISSION_GRANTED -> startListening()
                else -> requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is VoiceReminderUiState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = ""
                            binding.btnMic.contentDescription = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                        }
                        is VoiceReminderUiState.Listening -> {
                            binding.tvStatus.text = "Listening..."
                            binding.btnMic.contentDescription = "Listening..."
                            binding.btnMic.isEnabled = false
                        }
                        is VoiceReminderUiState.Processing -> {
                            binding.tvStatus.text = "Processing with AI..."
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is VoiceReminderUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = ""
                            binding.btnMic.contentDescription = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                            // ADD THIS ↓
                            viewLifecycleOwner.lifecycleScope.launch {
                                com.students.uniflow.data.repository.BurnoutRepository(requireContext())
                                    .logStudySession("VoiceReminder", 5)
                            }

                            loadSavedReminders()
                            viewModel.resetState()
                        }
                        is VoiceReminderUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = ""
                            binding.btnMic.contentDescription = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
        }
    }

    // Convert 24-hour "HH:mm" to 12-hour "h:mm AM/PM"
    private fun formatTo12Hour(time24: String): String {
        return try {
            val t = java.time.LocalTime.parse(
                time24,
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")
            )
            t.format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
        } catch (e: Exception) {
            time24
        }
    }

    private fun loadSavedReminders() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val db = AppDatabase.getInstance(requireContext())
                val saved = db.reminderDao().getAllReminders()
                val now = System.currentTimeMillis()
                remindersEntityList.clear()
                remindersEntityList.addAll(saved.filter { it.triggerAtMillis > now })
                refreshRemindersList()
            } catch (e: Exception) {
                android.util.Log.e("UNIFLOW_VOICE", "Load failed: ${e.message}")
            }
        }
    }

    private fun refreshRemindersList() {
        if (!isAdded || _binding == null) return

        binding.tvRemindersHeader.visibility =
            if (remindersEntityList.isEmpty()) View.GONE else View.VISIBLE

        binding.remindersContainer.removeAllViews()

        remindersEntityList.forEachIndexed { index, entity ->

            val card = com.google.android.material.card.MaterialCardView(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 12 }
                radius = 46f
                cardElevation = 2f
                strokeWidth = 2
                setCardBackgroundColor(android.graphics.Color.parseColor("#FDFAF7"))
                strokeColor = android.graphics.Color.parseColor("#EDD8D4")
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(44, 36, 44, 36)
            }

            // Number pill
            val tvNumber = TextView(requireContext()).apply {
                text = "REMINDER ${index + 1}"
                textSize = 9f
                typeface = resources.getFont(R.font.inter_medium)
                setTextColor(android.graphics.Color.parseColor("#B06060"))
                background = android.graphics.drawable.GradientDrawable().apply {
                    setColor(android.graphics.Color.parseColor("#FDE8E0"))
                    cornerRadius = 20f
                }
                setPadding(20, 8, 20, 8)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 10 }
            }

            val tvTask = TextView(requireContext()).apply {
                text = entity.task
                textSize = 15f
                typeface = resources.getFont(R.font.playfair_display_bold)
                setTextColor(android.graphics.Color.parseColor("#2A1010"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 8 }
            }

            // Format time as 12-hour before displaying
            val displayTime = formatTo12Hour(entity.time)

            val tvDateTime = TextView(requireContext()).apply {
                text = "📅  ${entity.date}    ⏰  $displayTime"
                textSize = 12f
                typeface = resources.getFont(R.font.inter_medium)
                setTextColor(android.graphics.Color.parseColor("#7A5050"))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.bottomMargin = 14 }
            }

            // Divider
            val divider = View(requireContext()).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                ).also { it.bottomMargin = 14 }
                setBackgroundColor(android.graphics.Color.parseColor("#EDD8D4"))
            }

            val btnRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
            }

            val btnDelete = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "Delete"
                textSize = 12f
                typeface = resources.getFont(R.font.inter_medium)
                setTextColor(android.graphics.Color.parseColor("#C04020"))
                strokeColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#EDD8D4")
                )
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 12 }
                setOnClickListener { confirmDelete(entity) }
            }

            val btnEdit = MaterialButton(
                requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = "Edit Time"
                textSize = 12f
                typeface = resources.getFont(R.font.inter_medium)
                setTextColor(android.graphics.Color.parseColor("#3D1010"))
                strokeColor = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#C8A8A0")
                )
                setOnClickListener { showEditDialog(entity) }
            }

            btnRow.addView(btnDelete)
            btnRow.addView(btnEdit)

            inner.addView(tvNumber)
            inner.addView(tvTask)
            inner.addView(tvDateTime)
            inner.addView(divider)
            inner.addView(btnRow)
            card.addView(inner)
            binding.remindersContainer.addView(card)
        }
    }

    private fun confirmDelete(entity: ReminderEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reminder")
            .setMessage("Delete reminder \"${entity.task}\"?")
            .setPositiveButton("Delete") { _, _ -> deleteReminder(entity) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReminder(entity: ReminderEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val intent = Intent(requireContext(), com.students.uniflow.utils.AlarmReceiver::class.java).apply {
                    action = "com.students.uniflow.ALARM_${entity.id}"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(), entity.id, intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }
                AppDatabase.getInstance(requireContext()).reminderDao().deleteById(entity.id)
                Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
                loadSavedReminders()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(entity: ReminderEntity) {
        val dialogLayout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 20, 60, 0)
        }

        val taskLabel = TextView(requireContext()).apply {
            text = "Task name"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#7A5050"))
            setPadding(0, 0, 0, 6)
        }

        val taskInput = android.widget.EditText(requireContext()).apply {
            hint = "Task name"
            setText(entity.task)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).also { it.bottomMargin = 20 }
        }

        val timeLabel = TextView(requireContext()).apply {
            text = "Time (HH:mm  e.g. 14:30)"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#7A5050"))
            setPadding(0, 0, 0, 6)
        }

        val timeInput = android.widget.EditText(requireContext()).apply {
            hint = "HH:mm"
            setText(entity.time)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        dialogLayout.addView(taskLabel)
        dialogLayout.addView(taskInput)
        dialogLayout.addView(timeLabel)
        dialogLayout.addView(timeInput)

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Reminder")
            .setView(dialogLayout)
            .setPositiveButton("Save") { _, _ ->
                val newTask = taskInput.text.toString().trim()
                val newTime = timeInput.text.toString().trim()
                when {
                    newTask.isEmpty() ->
                        Toast.makeText(requireContext(), "Task name cannot be empty", Toast.LENGTH_LONG).show()
                    !newTime.matches(Regex("\\d{2}:\\d{2}")) ->
                        Toast.makeText(requireContext(), "Invalid time format. Use HH:mm (e.g. 14:30)", Toast.LENGTH_LONG).show()
                    else ->
                        updateReminder(entity, newTask, newTime)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateReminder(entity: ReminderEntity, newTask: String, newTime: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val date = java.time.LocalDate.parse(
                    entity.date,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                )
                val time = java.time.LocalTime.parse(
                    newTime,
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )
                val newTriggerMillis = java.time.LocalDateTime.of(date, time)
                    .atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()

                if (newTriggerMillis <= System.currentTimeMillis()) {
                    Toast.makeText(requireContext(), "That time has already passed", Toast.LENGTH_LONG).show()
                    return@launch
                }

                // Cancel old alarm
                val oldIntent = Intent(requireContext(), com.students.uniflow.utils.AlarmReceiver::class.java).apply {
                    action = "com.students.uniflow.ALARM_${entity.id}"
                }
                val oldPending = PendingIntent.getBroadcast(
                    requireContext(), entity.id, oldIntent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (oldPending != null) {
                    val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(oldPending)
                }

                // Delete old, insert updated with new task name + time
                val db = AppDatabase.getInstance(requireContext())
                db.reminderDao().deleteById(entity.id)
                db.reminderDao().insertReminder(
                    ReminderEntity(
                        task = newTask,
                        date = entity.date,
                        time = newTime,
                        triggerAtMillis = newTriggerMillis
                    )
                )

                AlarmHelper.scheduleOneTimeReminder(requireContext(), newTask, newTriggerMillis)

                val display12 = formatTo12Hour(newTime)
                Toast.makeText(
                    requireContext(),
                    "Reminder updated to $display12",
                    Toast.LENGTH_SHORT
                ).show()
                loadSavedReminders()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun startListening() {
        viewModel.setListening()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechHelper.startListening(
            context = requireContext(),
            onResult = { spokenText ->
                requireActivity().runOnUiThread {
                    viewModel.processSpokenText(spokenText)
                }
            },
            onError = { errorMsg ->
                requireActivity().runOnUiThread {
                    viewModel.resetState()
                    Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        speechRecognizer?.destroy()
        speechRecognizer = null
        _binding = null
    }
}