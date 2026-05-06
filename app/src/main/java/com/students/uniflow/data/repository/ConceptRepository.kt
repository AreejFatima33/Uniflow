package com.students.uniflow.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.gson.Gson
import com.students.uniflow.BuildConfig
import com.students.uniflow.data.local.AppDatabase
import com.students.uniflow.data.local.entity.ConceptEntity
import com.students.uniflow.data.model.ConceptResult
import com.students.uniflow.utils.CacheHelper
import com.students.uniflow.utils.GeminiPrompts
import kotlinx.coroutines.delay

class ConceptRepository(private val context: Context) {

    private val gson = Gson()
    private val conceptDao = AppDatabase.getInstance(context).conceptDao()

    private val geminiVision = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY
    )

    suspend fun explainConcept(imageUri: Uri, userQuestion: String = ""): Result<ConceptResult> {
        return try {
            val bitmap = uriToBitmap(imageUri)
                ?: return Result.failure(Exception("Could not load image"))

            val prompt = GeminiPrompts.conceptSnap(userQuestion)

            val inputContent = content {
                image(bitmap)
                text(prompt)
            }

            val cacheKey = imageUri.toString()
            val rawResponse = CacheHelper.getCached(context, cacheKey, "concept")
                ?: sendVisionWithRetry(inputContent).also { response ->
                    CacheHelper.saveCache(context, cacheKey, "concept", response)
                }
            android.util.Log.d("UNIFLOW_CONCEPT", "Gemini Vision response: $rawResponse")

            val cleanJson = cleanGeminiJson(rawResponse)
            val result = gson.fromJson(cleanJson, ConceptResult::class.java)

            // Save to Room DB
            conceptDao.insert(
                ConceptEntity(
                    conceptTitle = result.conceptTitle,
                    simpleExplanation = result.simpleExplanation,
                    realLifeAnalogy = result.realLifeAnalogy,
                    keyPointsJson = gson.toJson(result.keyPoints),
                    examTip = result.examTip,
                    savedAt = System.currentTimeMillis()
                )
            )
            android.util.Log.d("UNIFLOW_CONCEPT", "Concept saved to DB ✅")

            Result.success(result)

        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_CONCEPT", "Error: ${e.message}")
            Result.failure(e)
        }
    }

    private fun uriToBitmap(uri: Uri): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                    decoder.isMutableRequired = true
                }
            } else {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            }
        } catch (e: Exception) {
            android.util.Log.e("UNIFLOW_CONCEPT", "Bitmap load error: ${e.message}")
            null
        }
    }

    private suspend fun sendVisionWithRetry(
        inputContent: com.google.ai.client.generativeai.type.Content,
        maxRetries: Int = 3
    ): String {
        var lastError: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                val response = geminiVision.generateContent(inputContent)
                val text = response.text ?: ""
                if (text.contains("503") || text.contains("UNAVAILABLE") || text.contains("Error:")) {
                    lastError = Exception("Gemini Vision error: $text")
                    delay(2000L * (attempt + 1))
                } else {
                    return text
                }
            } catch (e: Exception) {
                lastError = e
                delay(2000L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Gemini Vision failed after $maxRetries retries")
    }

    private fun cleanGeminiJson(raw: String): String {
        return raw
            .replace(Regex("```json\\s*"), "")
            .replace(Regex("```\\s*"), "")
            .trim()
            .let { s ->
                val start = s.indexOf('{')
                val end = s.lastIndexOf('}')
                if (start != -1 && end != -1 && end > start) s.substring(start, end + 1) else s
            }
    }
}