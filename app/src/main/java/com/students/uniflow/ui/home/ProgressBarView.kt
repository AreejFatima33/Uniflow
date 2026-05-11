package com.students.uniflow.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class ProgressBarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F0E0DC")
        style = Paint.Style.FILL
    }

    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val radius = h / 2f

        // Parse tag: "0.82|#2D8A4E"
        val tagStr = tag?.toString() ?: "0.5|#6B2A2A"
        val parts = tagStr.split("|")
        val fraction = parts.getOrNull(0)?.toFloatOrNull() ?: 0.5f
        val colorHex = parts.getOrNull(1) ?: "#6B2A2A"

        fillPaint.color = try {
            Color.parseColor(colorHex)
        } catch (e: Exception) {
            Color.parseColor("#6B2A2A")
        }

        // Draw track
        canvas.drawRoundRect(RectF(0f, 0f, w, h), radius, radius, trackPaint)

        // Draw fill
        val fillW = (w * fraction).coerceAtMost(w)
        if (fillW > 0f) {
            canvas.drawRoundRect(RectF(0f, 0f, fillW, h), radius, radius, fillPaint)
        }
    }
}