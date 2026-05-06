package com.students.uniflow.data.repository

import android.content.Context
import android.util.Log
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.GradeEntity
import com.students.uniflow.data.model.GradeResult
import com.students.uniflow.utils.GradeCalculator

class GradeRepository(private val context: Context) {

    private val gradeDao = AppDatabase.getInstance(context).gradeDao()

    suspend fun calculate(
        subject: String,
        quizMarks: Double, quizTotal: Double,
        assignmentMarks: Double, assignmentTotal: Double,
        midtermMarks: Double, midtermTotal: Double
    ): GradeResult {
        val result = GradeCalculator.calculate(
            quizMarks, quizTotal,
            assignmentMarks, assignmentTotal,
            midtermMarks, midtermTotal
        )

        // Save to Room DB
        val entity = GradeEntity(
            subject = subject,
            quizMarks = quizMarks,
            quizTotal = quizTotal,
            assignmentMarks = assignmentMarks,
            assignmentTotal = assignmentTotal,
            midtermMarks = midtermMarks,
            midtermTotal = midtermTotal,
            currentPercentage = result.currentPercentage
        )
        gradeDao.insertGrade(entity)
        Log.d("UNIFLOW_GRADE", "Grade saved to DB: ${result.currentPercentage}%")

        return result
    }
}