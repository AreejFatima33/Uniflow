package com.students.uniflow.ui.examoracle

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.repository.ExamOracleRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ExamOracleUiState {
    object Idle : ExamOracleUiState()
    object Loading : ExamOracleUiState()
    data class Success(val result: com.students.uniflow.data.model.ExamOracleResult) : ExamOracleUiState()
    data class Error(val message: String) : ExamOracleUiState()
}

class ExamOracleViewModel : ViewModel() {

    private lateinit var repository: ExamOracleRepository
    private var paperName: String = ""

    // Multi-paper: accumulate URIs before analyzing
    private val pendingUris = mutableListOf<android.net.Uri>()

    private var papersCount: Int = 0
    private val _uiState = MutableStateFlow<ExamOracleUiState>(ExamOracleUiState.Idle)
    val uiState: StateFlow<ExamOracleUiState> = _uiState

    fun getPapersCount(): Int = papersCount
    fun init(context: android.content.Context) {
        repository = ExamOracleRepository(context)
    }

    fun setPaperName(name: String) { paperName = name }
    fun getPaperName(): String = paperName

    // Just queue the URI — don't analyze yet
    fun addPaperImage(uri: Uri) {
        pendingUris.add(uri)
    }

    fun setPapersCount(count: Int) {
        papersCount = count
    }
    // Analyze all accumulated papers together
    fun analyzeAllPapers() {
        if (pendingUris.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = ExamOracleUiState.Loading
            val result = repository.processMultiplePapers(pendingUris.toList(), paperName)
            _uiState.value = if (result.isSuccess)
                ExamOracleUiState.Success(result.getOrThrow())
            else
                ExamOracleUiState.Error(result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    // Legacy single-paper path (kept for compatibility)
    fun processImage(uri: Uri) {
        pendingUris.clear()
        pendingUris.add(uri)
        analyzeAllPapers()
    }
}