package com.students.uniflow.data.model

// Gemini returns a list of predicted topics.
// Each topic has a name, probability label, and practice questions.
data class ExamOracleResult(
    val predictedTopics: List<PredictedTopic>
)

data class PredictedTopic(
    val topic: String,
    val probability: String,       // e.g. "High", "Medium", "Low"
    val practiceQuestions: List<String>
)