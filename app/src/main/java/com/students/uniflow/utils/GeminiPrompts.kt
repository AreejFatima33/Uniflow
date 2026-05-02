package com.students.uniflow.utils

import com.students.uniflow.data.local.entity.StudyLogEntity

object GeminiPrompts {

    // --- WEEK 3 ---

    fun lectureSnap(extractedText: String): String = """
        You are a study assistant. Read the student notes below and generate study materials.

        IMPORTANT: You MUST generate content. Empty responses are not acceptable.
        // Add this line inside the CRITICAL RULES of any prompt:
        - The text may be in English OR Urdu — process both languages equally
        - If the text is in Urdu, respond with topics and questions in Urdu as well
        Base ALL content strictly on the text between the markers below.
        The text may be upside down or partially garbled from OCR — do your best.
        Return ONLY raw JSON with NO markdown fences, NO backticks, NO explanation.

        {
          "title": "main topic from the text",
          "summary": "3-4 sentences summarizing what the text is about",
          "keyPoints": ["key point 1", "key point 2", "key point 3"],
          "flashcards": [
            {"question": "question about the text", "answer": "answer from the text"}
          ],
          "quizQuestions": [
            {
              "question": "question from the text",
              "options": ["A) option", "B) option", "C) option", "D) option"],
              "answer": "A) option"
            }
          ]
        }

        ===STUDENT NOTES START===
        $extractedText
        ===STUDENT NOTES END===
    """.trimIndent()

    fun timetableSnap(extractedText: String): String = """
        You are a timetable data extractor. Your ONLY job is to read the text below
        and extract class schedule entries.

        CRITICAL RULES:
        - Return ONLY a raw JSON array — no markdown, no backticks, no explanation
        - Extract ONLY what is actually written in the text — do NOT invent data
        - Each entry needs: day, time, subject, room, professor
        - For missing fields: use empty string ""
        - If the text has NO timetable data at all: return exactly []

        Example output:
        [{"day":"Monday","time":"08:00 AM","subject":"Math","room":"101","professor":"Sir Ahmed"}]

        Timetable text to extract from:
        ---
        $extractedText
        ---
    """.trimIndent()

    // --- WEEK 4 ---

    fun examOracle(extractedText: String, paperName: String = ""): String {
        val subjectLine = if (paperName.isNotBlank()) "Subject: $paperName\n" else ""
        return """
        You are an expert exam analyst. Analyze this past exam paper and predict the most likely topics.
        ${subjectLine}
        Exam paper text:
        $extractedText
        
        You are an expert exam analyst. A student has provided text from their past examination papers.
        Analyze the content carefully and identify which topics appear most frequently.

        CRITICAL RULES:
        - If the text is garbled or partially unreadable (OCR errors), do your BEST to extract meaning from partial words
        - If text appears to be in Urdu script but shows as garbled characters, note that Urdu content was detected
        - The text may be in English OR Urdu — process both languages equally
        - If the text is in Urdu, respond with topics and questions in Urdu as well
        - Return ONLY a raw JSON object — no markdown, no backticks, no explanation
        - Base your analysis ONLY on the text provided below — do NOT invent topics
        - Identify exactly 5 predicted topics (or fewer if there are not enough topics in the text)
        - For each topic, assign a probability: "High", "Medium", or "Low"
        - For each topic, generate exactly 3 practice questions based on the text
        - probability = "High" means the topic appears 3+ times in the papers
        - probability = "Medium" means it appears 2 times
        - probability = "Low" means it appears once but is important

        Return this exact JSON structure:
        {
          "predictedTopics": [
            {
              "topic": "topic name here",
              "probability": "High",
              "practiceQuestions": [
                "practice question 1",
                "practice question 2",
                "practice question 3"
              ]
            }
          ]
        }

        ===PAST PAPER TEXT START===
        $extractedText
        ===PAST PAPER TEXT END===
    """.trimIndent()
    }

    fun deadlineGenie(extractedText: String, examDate: String, subjectName: String): String {
        val today = java.time.LocalDate.now()
        val currentYear = today.year

        // Normalize input: trim, capitalize first letter of month, append year if missing
        val normalizedInput = examDate.trim()
            .replaceFirstChar { it.uppercase() }
            .let { s ->
                // If the string has no 4-digit year, append current year
                if (!s.contains(Regex("\\b\\d{4}\\b"))) "$s $currentYear" else s
            }

        android.util.Log.d("UNIFLOW_DATE", "Raw exam date input: '$examDate'")
        android.util.Log.d("UNIFLOW_DATE", "Normalized: '$normalizedInput'")

        val exam = listOf(
            // With year
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "dd-MM-yyyy",
            "d MMM yyyy",
            "dd MMM yyyy",
            "d MMMM yyyy",
            "dd MMMM yyyy",
            "MMMM d yyyy",
            "MMMM dd yyyy",
            // Without year (year will be appended above so these become "X month YYYY")
            "d MMM yyyy",
            "d MMMM yyyy",
            "dd MMM yyyy",
            "dd MMMM yyyy"
        ).firstNotNullOfOrNull { pattern ->
            try {
                java.time.LocalDate.parse(
                    normalizedInput,
                    java.time.format.DateTimeFormatter.ofPattern(pattern, java.util.Locale.ENGLISH)
                )
            } catch (e: Exception) { null }
        }

        // Last resort: try to extract day and month using regex
        val parsedExam = exam ?: run {
            val dayMonthRegex = Regex("""(\d{1,2})\s+([A-Za-z]+)""")
            val match = dayMonthRegex.find(normalizedInput)
            if (match != null) {
                val day = match.groupValues[1].toIntOrNull()
                val monthStr = match.groupValues[2].lowercase()
                    .replaceFirstChar { it.uppercase() }
                val monthMap = mapOf(
                    "Jan" to 1, "Feb" to 2, "Mar" to 3, "Apr" to 4,
                    "May" to 5, "Jun" to 6, "Jul" to 7, "Aug" to 8,
                    "Sep" to 9, "Oct" to 10, "Nov" to 11, "Dec" to 12,
                    "January" to 1, "February" to 2, "March" to 3, "April" to 4,
                    "June" to 6, "July" to 7, "August" to 8, "September" to 9,
                    "October" to 10, "November" to 11, "December" to 12
                )
                val monthNum = monthMap.entries.firstOrNull {
                    monthStr.startsWith(it.key, ignoreCase = true)
                }?.value
                if (day != null && monthNum != null) {
                    try {
                        java.time.LocalDate.of(currentYear, monthNum, day)
                    } catch (e: Exception) { null }
                } else null
            } else null
        } ?: today.plusDays(14).also {
            android.util.Log.w("UNIFLOW_DATE", "Could not parse '$examDate', falling back to +14 days")
        }

        android.util.Log.d("UNIFLOW_DATE", "Final exam date: $parsedExam")

        val formatter = java.time.format.DateTimeFormatter.ofPattern(
            "EEEE, MMMM d, yyyy", java.util.Locale.ENGLISH
        )

        val daysBetween = java.time.temporal.ChronoUnit.DAYS
            .between(today, parsedExam).toInt().coerceAtLeast(1)

        val dateList = (0 until daysBetween).joinToString("\n") { i ->
            "Day ${i + 1}: ${today.plusDays(i.toLong()).format(formatter)}"
        }

        return """
You are a study planner. Create a day-by-day study plan based on the syllabus below.

Subject: $subjectName
Exam Date: ${parsedExam.format(formatter)}
Today: ${today.format(formatter)}

IMPORTANT: Use EXACTLY these pre-calculated dates. Do NOT change them:
$dateList

Total days available: $daysBetween
The plan must have EXACTLY $daysBetween entries — one per day listed above. No more, no less.

Syllabus text:
$extractedText

Return ONLY valid JSON, no markdown, no backticks, no explanation:
{
  "planTitle": "Study Plan for $subjectName",
  "totalDays": $daysBetween,
  "dailyPlan": [
    {
      "day": 1,
      "date": "Monday, April 28, 2026",
      "topic": "Topic name here",
      "tasks": ["Task 1", "Task 2"]
    }
  ]
}
    """.trimIndent()
    }

// ── Week 5 ──────────────────────────────────────────────────────────────

fun burnoutRadar(logs: List<StudyLogEntity>, totalMinutesThisWeek: Int): String {
    val logSummary = logs.joinToString("\n") { log ->
        "Date: ${log.date}, Minutes studied: ${log.studyMinutes}, Feature used: ${log.featureUsed}"
    }
    val totalHours = totalMinutesThisWeek / 60.0

    return """
            You are a student wellbeing advisor. Analyze the study log below and detect burnout risk.

            Study activity from the last 7 days:
            $logSummary

            Total hours studied this week: ${"%.1f".format(totalHours)} hours

            Based on this data, return ONLY raw JSON with NO markdown, NO backticks, NO explanation:
            {
              "riskLevel": "High",
              "summary": "2-3 sentences explaining the burnout risk level based on the data",
              "suggestions": [
                "specific actionable advice 1",
                "specific actionable advice 2",
                "specific actionable advice 3"
              ],
              "encouragement": "one warm, caring sentence to motivate the student"
            }

            riskLevel must be exactly one of: "High", "Medium", "Low"
            - High: studying more than 6 hours/day consistently, or no breaks detected
            - Medium: studying 3-6 hours/day, some irregularity
            - Low: balanced study pattern, appropriate breaks
        """.trimIndent()
}

fun conceptSnap(userQuestion: String = ""): String = """
        You are a teacher who explains complex concepts in simple, friendly language.
        A student has photographed something from their textbook — a diagram, formula, algorithm, or concept.

        ${if (userQuestion.isNotBlank()) "The student's question: $userQuestion" else "Explain what you see in the image."}

        Return ONLY raw JSON with NO markdown, NO backticks, NO explanation:
        {
          "conceptTitle": "name of the concept or diagram",
          "simpleExplanation": "explain in 3-4 sentences as if talking to a beginner",
          "realLifeAnalogy": "one real-life analogy that makes it easy to understand",
          "keyPoints": ["important point 1", "important point 2", "important point 3"],
          "examTip": "one sentence tip on how this concept is typically tested in exams"
        }
    """.trimIndent()
}
