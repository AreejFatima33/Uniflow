package com.students.uniflow.utils

import com.students.uniflow.data.model.GradeResult

object GradeCalculator {

    // Standard Pakistani university grading weights:
    // Quiz = 10%, Assignment = 10%, Midterm = 30%, Final = 50%
    private const val QUIZ_WEIGHT = 0.10
    private const val ASSIGNMENT_WEIGHT = 0.10
    private const val MIDTERM_WEIGHT = 0.30
    private const val FINAL_WEIGHT = 0.50

    fun calculate(
        quizMarks: Double, quizTotal: Double,
        assignmentMarks: Double, assignmentTotal: Double,
        midtermMarks: Double, midtermTotal: Double
    ): GradeResult {

        // Avoid division by zero
        val quizPct = if (quizTotal > 0) (quizMarks / quizTotal) * 100 else 0.0
        val assignPct = if (assignmentTotal > 0) (assignmentMarks / assignmentTotal) * 100 else 0.0
        val midPct = if (midtermTotal > 0) (midtermMarks / midtermTotal) * 100 else 0.0

        // Current weighted score (without final)
        val currentScore = (quizPct * QUIZ_WEIGHT) +
                (assignPct * ASSIGNMENT_WEIGHT) +
                (midPct * MIDTERM_WEIGHT)

        // To get grade X: currentScore + (finalPct * 0.50) >= X
        // → finalPct >= (X - currentScore) / 0.50
        val requiredForA = calculateRequired(currentScore, 85.0)
        val requiredForB = calculateRequired(currentScore, 70.0)
        val requiredForC = calculateRequired(currentScore, 55.0)

        // Current letter grade (if final exam were 0)
        val currentGrade = when {
            currentScore >= 42.5 -> "On track for A"  // 85% of non-final 50%
            currentScore >= 35.0 -> "On track for B"
            currentScore >= 27.5 -> "On track for C"
            else -> "At risk of failing"
        }

        return GradeResult(
            currentPercentage = currentScore,
            requiredForA = requiredForA,
            requiredForB = requiredForB,
            requiredForC = requiredForC,
            grade = currentGrade
        )
    }

    private fun calculateRequired(currentScore: Double, targetTotal: Double): Double {
        val required = (targetTotal - currentScore) / FINAL_WEIGHT
        return when {
            required <= 0.0 -> 0.0       // already secured
            required > 100.0 -> -1.0     // impossible even with 100%
            else -> required
        }
    }
}