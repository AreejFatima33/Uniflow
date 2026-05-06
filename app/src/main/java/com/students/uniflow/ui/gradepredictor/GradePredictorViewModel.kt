package com.students.uniflow.ui.gradepredictor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.students.uniflow.data.model.GradeResult
import com.students.uniflow.data.repository.GradeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class GradePredictorUiState {
    object Idle : GradePredictorUiState()
    object Loading : GradePredictorUiState()
    data class Success(val result: GradeResult) : GradePredictorUiState()
    data class Error(val message: String) : GradePredictorUiState()
}

class GradePredictorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GradeRepository(application)

    private val _uiState = MutableStateFlow<GradePredictorUiState>(GradePredictorUiState.Idle)
    val uiState: StateFlow<GradePredictorUiState> = _uiState

    fun calculateGrade(
        subject: String,
        quizMarks: Double, quizTotal: Double,
        assignmentMarks: Double, assignmentTotal: Double,
        midtermMarks: Double, midtermTotal: Double
    ) {
        viewModelScope.launch {
            _uiState.value = GradePredictorUiState.Loading
            try {
                val result = repository.calculate(
                    subject,
                    quizMarks, quizTotal,
                    assignmentMarks, assignmentTotal,
                    midtermMarks, midtermTotal
                )
                _uiState.value = GradePredictorUiState.Success(result)
            } catch (e: Exception) {
                _uiState.value = GradePredictorUiState.Error(e.message ?: "Calculation failed")
            }
        }
    }
}