package com.students.uniflow.ui.burnoutradar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.BurnoutResult
import com.students.uniflow.data.repository.BurnoutRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class BurnoutUiState {
    object Idle : BurnoutUiState()
    object Loading : BurnoutUiState()
    data class Success(val result: BurnoutResult) : BurnoutUiState()
    data class Error(val message: String) : BurnoutUiState()
}

class BurnoutRadarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BurnoutRepository(application)

    private val _uiState = MutableStateFlow<BurnoutUiState>(BurnoutUiState.Idle)
    val uiState: StateFlow<BurnoutUiState> = _uiState

    fun analyzeBurnout() {
        viewModelScope.launch {
            _uiState.value = BurnoutUiState.Loading
            val result = repository.analyzeBurnout()
            result.onSuccess { burnoutResult ->
                _uiState.value = BurnoutUiState.Success(burnoutResult)
            }.onFailure { error ->
                _uiState.value = BurnoutUiState.Error(error.message ?: "Unknown error")
            }
        }
    }
}