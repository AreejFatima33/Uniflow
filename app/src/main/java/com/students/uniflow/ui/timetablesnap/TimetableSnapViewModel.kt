package com.students.uniflow.ui.timetablesnap

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.students.uniflow.data.model.TimetableEntry
import com.students.uniflow.data.repository.TimetableRepository
import kotlinx.coroutines.launch

class TimetableSnapViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = TimetableRepository(app)

    private val _uiState = MutableLiveData<TimetableUiState>(TimetableUiState.Idle)
    val uiState: LiveData<TimetableUiState> = _uiState

    fun processImage(uri: Uri) {
        _uiState.value = TimetableUiState.Loading
        viewModelScope.launch {
            val result = repo.processTimetableImage(uri)
            _uiState.value = if (result.isSuccess)
                TimetableUiState.Success(result.getOrThrow())
            else
                TimetableUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
}

sealed class TimetableUiState {
    object Idle    : TimetableUiState()
    object Loading : TimetableUiState()
    data class Success(val entries: List<TimetableEntry>) : TimetableUiState()
    data class Error(val message: String)                  : TimetableUiState()
}