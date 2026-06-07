package com.students.uniflow.data.model


data class NoteResult(
    val title: String = "",
    val summary: String = "",
    val keyPoints: List<String> = emptyList(),
    val flashcards: List<Flashcard> = emptyList(),
    val quizQuestions: List<QuizQuestion> = emptyList()
)

data class Flashcard(
    val question: String = "",
    val answer: String = ""
)

data class QuizQuestion(
    val question: String = "",
    val options: List<String> = emptyList(),
    val answer: String = ""
)