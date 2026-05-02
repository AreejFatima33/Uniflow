package com.students.uniflow.ui.examoracle

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.ExamOracleResult
import com.students.uniflow.data.repository.ExamOracleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ExamOracleUiState {
    object Idle : ExamOracleUiState()
    object Loading : ExamOracleUiState()
    data class Success(val result: ExamOracleResult) : ExamOracleUiState()
    data class Error(val message: String) : ExamOracleUiState()
}

class ExamOracleViewModel : ViewModel() {

    private lateinit var repository: ExamOracleRepository
    private var paperName: String = ""

    private val _uiState = MutableStateFlow<ExamOracleUiState>(ExamOracleUiState.Idle)
    val uiState: StateFlow<ExamOracleUiState> = _uiState

    fun init(context: android.content.Context) {
        repository = ExamOracleRepository(context)
    }

    fun setPaperName(name: String) {
        paperName = name
    }

    fun processImage(uri: Uri) {
        viewModelScope.launch {
            _uiState.value = ExamOracleUiState.Loading
            val result = repository.processExamPaper(uri, paperName)
            _uiState.value = if (result.isSuccess)
                ExamOracleUiState.Success(result.getOrThrow())
            else
                ExamOracleUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }
    fun getPaperName(): String = paperName
}