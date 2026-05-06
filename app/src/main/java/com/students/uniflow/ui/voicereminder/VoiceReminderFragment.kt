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
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.ReminderEntity
import com.students.uniflow.data.model.ReminderResult
import com.students.uniflow.databinding.FragmentVoiceReminderBinding
import com.students.uniflow.utils.AlarmHelper
import com.students.uniflow.utils.SpeechHelper
import kotlinx.coroutines.launch

class VoiceReminderFragment : Fragment() {

    private var _binding: FragmentVoiceReminderBinding? = null
    private val binding get() = _binding!!
    private val viewModel: VoiceReminderViewModel by viewModels()
    private var speechRecognizer: SpeechRecognizer? = null

    // Stores reminder entities so we can delete/edit by id
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
                            binding.btnMic.text = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                        }
                        is VoiceReminderUiState.Listening -> {
                            binding.tvStatus.text = "🎙️ Listening..."
                            binding.btnMic.text = "Listening..."
                            binding.btnMic.isEnabled = false
                        }
                        is VoiceReminderUiState.Processing -> {
                            binding.tvStatus.text = "Processing with AI..."
                            binding.progressBar.visibility = View.VISIBLE
                        }
                        is VoiceReminderUiState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = ""
                            binding.btnMic.text = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                            // Reload from DB to get the new entity with its real id
                            loadSavedReminders()
                            viewModel.resetState()
                        }
                        is VoiceReminderUiState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.tvStatus.text = ""
                            binding.btnMic.text = "Tap to Speak"
                            binding.btnMic.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                            viewModel.resetState()
                        }
                    }
                }
            }
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
                ).also { it.bottomMargin = 16 }
                radius = 12f
                cardElevation = 4f
            }

            val inner = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 30, 40, 30)
            }

            val tvNumber = TextView(requireContext()).apply {
                text = "Reminder ${index + 1}"
                textSize = 12f
                setTextColor(android.graphics.Color.GRAY)
            }

            val tvTask = TextView(requireContext()).apply {
                text = "📌 ${entity.task}"
                textSize = 16f
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding(0, 8, 0, 4)
            }

            val tvDateTime = TextView(requireContext()).apply {
                text = "🗓 ${entity.date}  ⏰ ${entity.time}"
                textSize = 14f
            }

            // Button row
            val btnRow = LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(0, 16, 0, 0)
            }

            val btnDelete = MaterialButton(requireContext(),null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Delete"
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = 16 }
                setOnClickListener { confirmDelete(entity) }
            }

            val btnEdit = MaterialButton(requireContext(), null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
                text = "Edit Time"
                textSize = 12f
                setOnClickListener { showEditDialog(entity) }
            }

            btnRow.addView(btnDelete)
            btnRow.addView(btnEdit)

            inner.addView(tvNumber)
            inner.addView(tvTask)
            inner.addView(tvDateTime)
            inner.addView(btnRow)
            card.addView(inner)
            binding.remindersContainer.addView(card)
        }
    }

    private fun confirmDelete(entity: ReminderEntity) {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Reminder")
            .setMessage("Delete reminder \"${entity.task}\"?")
            .setPositiveButton("Delete") { _, _ ->
                deleteReminder(entity)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteReminder(entity: ReminderEntity) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Cancel the alarm in AlarmManager
                val intent = Intent(requireContext(), com.students.uniflow.utils.AlarmReceiver::class.java).apply {
                    action = "com.students.uniflow.ALARM_${entity.id}"
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    requireContext(),
                    entity.id,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (pendingIntent != null) {
                    val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                }

                // Delete from DB
                AppDatabase.getInstance(requireContext()).reminderDao().deleteById(entity.id)

                Toast.makeText(requireContext(), "Reminder deleted", Toast.LENGTH_SHORT).show()
                loadSavedReminders()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Failed to delete", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEditDialog(entity: ReminderEntity) {
        val timeInput = android.widget.EditText(requireContext()).apply {
            hint = "New time (e.g. 14:30)"
            setText(entity.time)
            inputType = android.text.InputType.TYPE_CLASS_TEXT
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Edit Reminder Time")
            .setMessage("Task: ${entity.task}\nDate: ${entity.date}\n\nEnter new time (HH:mm):")
            .setView(timeInput)
            .setPositiveButton("Save") { _, _ ->
                val newTime = timeInput.text.toString().trim()
                if (newTime.matches(Regex("\\d{2}:\\d{2}"))) {
                    updateReminder(entity, newTime)
                } else {
                    Toast.makeText(requireContext(), "Invalid time format. Use HH:mm (e.g. 14:30)", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateReminder(entity: ReminderEntity, newTime: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Calculate new trigger time
                val date = java.time.LocalDate.parse(entity.date,
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                val time = java.time.LocalTime.parse(newTime,
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
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

                // Delete old, insert updated record
                val db = AppDatabase.getInstance(requireContext())
                db.reminderDao().deleteById(entity.id)
                db.reminderDao().insertReminder(
                    ReminderEntity(
                        task = entity.task,
                        date = entity.date,
                        time = newTime,
                        triggerAtMillis = newTriggerMillis
                    )
                )

                // Schedule new alarm
                AlarmHelper.scheduleOneTimeReminder(requireContext(), entity.task, newTriggerMillis)

                Toast.makeText(requireContext(), "Reminder updated to $newTime", Toast.LENGTH_SHORT).show()
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