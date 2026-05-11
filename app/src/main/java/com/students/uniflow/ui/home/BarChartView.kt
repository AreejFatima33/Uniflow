package com.students.uniflow.ui.home

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val labels = listOf("Jan", "Mar", "May", "Jul", "Aug", "Sep")
    private val values = listOf(0.45f, 0.60f, 0.88f, 0.52f, 0.70f, 0.58f)
    private val activeIndex = 2  // May = highlighted

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#6B2A2A")
        style = Paint.Style.FILL
    }

    private val activeBarPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0453A")
        style = Paint.Style.FILL
    }

    private val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#9A7070")
        textSize = 24f
        textAlign = Paint.Align.CENTER
    }

    private val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#C0453A")
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 20f
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val w = width.toFloat()
        val h = height.toFloat()
        val labelHeight = 36f
        val chartHeight = h - labelHeight
        val barAreaTop = 20f
        val maxBarHeight = chartHeight - barAreaTop - labelHeight

        val totalBars = values.size
        val barWidth = w / (totalBars * 2f)
        val gap = barWidth

        values.forEachIndexed { i, value ->
            val barH = value * maxBarHeight
            val centerX = gap + i * (barWidth + gap) + barWidth / 2f
            val left = centerX - barWidth / 2f
            val right = centerX + barWidth / 2f
            val top = chartHeight - barH - labelHeight
            val bottom = chartHeight - labelHeight

            val paint = if (i == activeIndex) activeBarPaint else barPaint
            val rect = RectF(left, top, right, bottom)
            canvas.drawRoundRect(rect, 8f, 8f, paint)

            // Badge on active bar
            if (i == activeIndex) {
                val badgeText = "4.0.670"
                val badgeW = badgeTextPaint.measureText(badgeText) + 20f
                val badgeH = 32f
                val badgeLeft = centerX - badgeW / 2f
                val badgeRight = centerX + badgeW / 2f
                val badgeTop = top - badgeH - 6f
                val badgeBottom = top - 6f
                canvas.drawRoundRect(
                    RectF(badgeLeft, badgeTop, badgeRight, badgeBottom),
                    6f, 6f, badgePaint
                )
                canvas.drawText(
                    badgeText,
                    centerX,
                    badgeBottom - (badgeH / 2f) + badgeTextPaint.textSize / 3f,
                    badgeTextPaint
                )
            }

            // Label
            canvas.drawText(
                labels[i],
                centerX,
                h - 4f,
                labelPaint
            )
        }
    }
}