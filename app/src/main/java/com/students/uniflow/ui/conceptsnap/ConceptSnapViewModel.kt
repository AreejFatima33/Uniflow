package com.students.uniflow.ui.conceptsnap

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.ConceptResult
import com.students.uniflow.data.repository.ConceptRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConceptUiState {
    object Idle : ConceptUiState()
    object Loading : ConceptUiState()
    data class Success(val result: ConceptResult) : ConceptUiState()
    data class Error(val message: String) : ConceptUiState()
}

class ConceptSnapViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ConceptRepository(application)

    private val _uiState = MutableStateFlow<ConceptUiState>(ConceptUiState.Idle)
    val uiState: StateFlow<ConceptUiState> = _uiState

    fun explainConcept(imageUri: Uri, userQuestion: String = "") {
        viewModelScope.launch {
            _uiState.value = ConceptUiState.Loading
            val result = repository.explainConcept(imageUri, userQuestion)
            result.onSuccess { conceptResult ->
                _uiState.value = ConceptUiState.Success(conceptResult)
            }.onFailure { error ->
                _uiState.value = ConceptUiState.Error(error.message ?: "Unknown error")
            }
        }
    }

    fun reset() {
        _uiState.value = ConceptUiState.Idle
    }
}