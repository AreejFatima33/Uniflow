package com.students.uniflow.ui.voicereminder

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.ReminderResult
import com.students.uniflow.data.repository.ReminderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class VoiceReminderUiState {
    object Idle : VoiceReminderUiState()
    object Listening : VoiceReminderUiState()
    object Processing : VoiceReminderUiState()
    data class Success(val result: ReminderResult) : VoiceReminderUiState()
    data class Error(val message: String) : VoiceReminderUiState()
}

class VoiceReminderViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ReminderRepository(application)

    private val _uiState = MutableStateFlow<VoiceReminderUiState>(VoiceReminderUiState.Idle)
    val uiState: StateFlow<VoiceReminderUiState> = _uiState

    fun setListening() {
        _uiState.value = VoiceReminderUiState.Listening
    }

    fun processSpokenText(spokenText: String) {
        viewModelScope.launch {
            _uiState.value = VoiceReminderUiState.Processing
            try {
                val result = repository.processVoiceInput(spokenText)
                _uiState.value = VoiceReminderUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = VoiceReminderUiState.Error(e.message ?: "Failed to set reminder")
            }
        }
    }

    fun resetState() {
        _uiState.value = VoiceReminderUiState.Idle
    }
}