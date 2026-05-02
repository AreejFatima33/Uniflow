package com.students.uniflow.data.model

data class ConceptResult(
    val conceptTitle: String,
    val simpleExplanation: String,
    val realLifeAnalogy: String,
    val keyPoints: List<String>,
    val examTip: String
)