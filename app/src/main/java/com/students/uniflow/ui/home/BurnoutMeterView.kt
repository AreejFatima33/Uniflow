package com.students.uniflow.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BurnoutMeterView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#F0E0DC")
        strokeCap = Paint.Cap.ROUND
    }

    private val lowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 14f
        color = Color.parseColor("#2D8A4E")
        strokeCap = Paint.Cap.ROUND
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2D8A4E")
        textSize = 28f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val subPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9A7070")
        textSize = 18f
        textAlign = Paint.Align.CENTER
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val margin = 20f
        val oval = RectF(margin, margin, w - margin, h * 1.6f)

        // Track (full arc)
        canvas.drawArc(oval, 180f, 180f, false, trackPaint)
        // Fill — low risk = 30% of arc
        canvas.drawArc(oval, 180f, 54f, false, lowPaint)

        // Label
        canvas.drawText("LOW", w / 2f, h - 6f, labelPaint)
        canvas.drawText("Risk", w / 2f, h + 14f, subPaint)
    }
}