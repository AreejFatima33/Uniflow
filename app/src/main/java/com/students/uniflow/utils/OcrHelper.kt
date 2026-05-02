package com.students.uniflow.utils

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

object OcrHelper {

    fun extractTextFromImage(
        context: Context,
        imageUri: Uri,
        onSuccess: (String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val image = try {
            InputImage.fromFilePath(context, imageUri)
        } catch (e: Exception) {
            onError(e)
            return
        }

        // Single recognizer — ML Kit auto-detects Latin + Arabic/Urdu script
        // when text-recognition-arabic is on the classpath
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text
                android.util.Log.d("UNIFLOW_OCR", "Extracted: $text")
                onSuccess(text)
            }
            .addOnFailureListener { e ->
                android.util.Log.e("UNIFLOW_OCR", "OCR failed: ${e.message}")
                onError(e)
            }
    }
}