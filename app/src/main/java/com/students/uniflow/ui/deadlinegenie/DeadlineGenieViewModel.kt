package com.students.uniflow.ui.deadlinegenie

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.StudyPlanResult
import com.students.uniflow.data.repository.StudyPlanRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class DeadlineGenieUiState {
    object Idle : DeadlineGenieUiState()
    object Loading : DeadlineGenieUiState()
    data class Success(val result: StudyPlanResult) : DeadlineGenieUiState()
    data class Error(val message: String) : DeadlineGenieUiState()
}

class DeadlineGenieViewModel : ViewModel() {

    private lateinit var repository: StudyPlanRepository

    private val _uiState = MutableStateFlow<DeadlineGenieUiState>(DeadlineGenieUiState.Idle)
    val uiState: StateFlow<DeadlineGenieUiState> = _uiState

    fun init(context: android.content.Context) {
        repository = StudyPlanRepository(context)
    }

    fun processSyllabus(uri: Uri, examDate: String, subjectName: String) {
        viewModelScope.launch {
            _uiState.value = DeadlineGenieUiState.Loading
            val result = repository.processSyllabus(uri, examDate, subjectName)
            _uiState.value = if (result.isSuccess)
                DeadlineGenieUiState.Success(result.getOrThrow())
            else
                DeadlineGenieUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
}