package com.students.uniflow.ui.lecturesnap

import android.app.Application
import android.net.Uri
import androidx.lifecycle.*
import com.students.uniflow.data.model.NoteResult
import com.students.uniflow.data.repository.LectureRepository
import kotlinx.coroutines.launch

class LectureSnapViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = LectureRepository(app)

    private val _uiState = MutableLiveData<LectureUiState>(LectureUiState.Idle)
    val uiState: LiveData<LectureUiState> = _uiState

    fun processImage(uri: Uri) {
        _uiState.value = LectureUiState.Loading
        viewModelScope.launch {
            val result = repo.processLectureImage(uri)
            _uiState.value = if (result.isSuccess)
                LectureUiState.Success(result.getOrThrow())
            else
                LectureUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
}

sealed class LectureUiState {
    object Idle    : LectureUiState()
    object Loading : LectureUiState()
    data class Success(val result: NoteResult) : LectureUiState()
    data class Error(val message: String)      : LectureUiState()
}