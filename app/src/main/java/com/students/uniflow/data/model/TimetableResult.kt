package com.students.uniflow.data.model

// Shape of JSON returned by Gemini for TimetableSnap
// Example JSON:
// [
//   {"day":"Monday","time":"09:00 AM","subject":"OOP","room":"CS-1","professor":"Sir Ali"},
//   {"day":"Monday","time":"11:00 AM","subject":"DSA","room":"CS-2","professor":"Ma'am Sara"}
// ]

data class TimetableEntry(
    val day: String = "",
    val time: String = "",
    val subject: String = "",
    val room: String = "",
    val professor: String = ""
)