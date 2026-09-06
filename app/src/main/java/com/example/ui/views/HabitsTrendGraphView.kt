package com.example.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.DayHabitGraphData

class HabitsTrendGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var graphData: List<DayHabitGraphData> = emptyList()
    private var selectedIndex: Int = -1

    var onDaySelectedListener: ((DayHabitGraphData) -> Unit)? = null

    // Colors resolved dynamically
    private val colorGrid get() = ContextCompat.getColor(context, R.color.app_stroke)
    private val colorTextMuted get() = ContextCompat.getColor(context, R.color.app_text_muted)
    private val colorTextPrimary get() = ContextCompat.getColor(context, R.color.app_text_primary)
    private val colorTrack get() = ContextCompat.getColor(context, R.color.app_surface_variant)
    private val colorBarToday get() = ContextCompat.getColor(context, R.color.app_text_primary)
    private val colorBarPast get() = ContextCompat.getColor(context, R.color.app_text_secondary)
    private val colorHighlightBorder get() = ContextCompat.getColor(context, R.color.app_text_primary)

    // Paints
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(11f)
        textAlign = Paint.Align.CENTER
    }

    private val labelAxisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(10f)
        textAlign = Paint.Align.LEFT
    }

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val highlightStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = dpToPx(2f)
        style = Paint.Style.STROKE
    }

    private val barRect = RectF()
    private val trackRect = RectF()

    fun setData(data: List<DayHabitGraphData>) {
        this.graphData = data
        // Select today by default if available
        val todayIdx = data.indexOfFirst { it.isToday }
        if (todayIdx >= 0) {
            selectedIndex = todayIdx
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        gridPaint.color = colorGrid
        labelAxisPaint.color = colorTextMuted
        trackPaint.color = colorTrack
        highlightStrokePaint.color = colorHighlightBorder

        if (graphData.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextMuted
                textSize = dpToPx(13f)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No habit data recorded yet", width / 2f, height / 2f, emptyPaint)
            return
        }

        val topPadding = dpToPx(32f)
        val bottomPadding = dpToPx(30f)
        val leftPadding = dpToPx(36f)
        val rightPadding = dpToPx(16f)

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        if (chartHeight <= 0 || chartWidth <= 0) return

        // 1. Draw horizontal guide lines & labels for 100%, 50%, 0%
        val levels = listOf(1.0f to "100%", 0.5f to "50%", 0.0f to "0%")
        for ((ratio, label) in levels) {
            val y = topPadding + chartHeight * (1f - ratio)
            canvas.drawLine(leftPadding, y, width - rightPadding, y, gridPaint)
            canvas.drawText(label, dpToPx(4f), y + dpToPx(4f), labelAxisPaint)
        }

        // 2. Draw 7 day bars
        val itemCount = graphData.size
        val columnWidth = chartWidth / itemCount.toFloat()
        val barWidth = dpToPx(22f).coerceAtMost(columnWidth * 0.65f)
        val cornerRadius = barWidth / 2f

        for (i in 0 until itemCount) {
            val item = graphData[i]
            val centerX = leftPadding + (i + 0.5f) * columnWidth

            trackRect.set(
                centerX - barWidth / 2f,
                topPadding,
                centerX + barWidth / 2f,
                topPadding + chartHeight
            )
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

            // Draw filled bar based on completion percentage
            val fillHeight = (item.completionRatePercent / 100f).coerceIn(0f, 1f) * chartHeight
            if (fillHeight > 0f) {
                barRect.set(
                    centerX - barWidth / 2f,
                    (topPadding + chartHeight) - fillHeight,
                    centerX + barWidth / 2f,
                    topPadding + chartHeight
                )
                barPaint.color = if (item.isToday) colorBarToday else colorBarPast
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }

            // Selection indicator or today ring
            if (i == selectedIndex) {
                canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, highlightStrokePaint)
            }

            // Text on top of bar (percentage)
            textPaint.color = if (item.isToday) colorTextPrimary else colorTextMuted
            textPaint.isFakeBoldText = item.isToday || i == selectedIndex
            val percentText = "${item.completionRatePercent}%"
            canvas.drawText(percentText, centerX, topPadding - dpToPx(8f), textPaint)

            // Day label below bar
            val dayText = if (item.isToday) "Today" else item.dayLabel
            canvas.drawText(dayText, centerX, height - dpToPx(8f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            if (graphData.isEmpty()) return super.onTouchEvent(event)

            val leftPadding = dpToPx(36f)
            val rightPadding = dpToPx(16f)
            val chartWidth = width - leftPadding - rightPadding
            val columnWidth = chartWidth / graphData.size.toFloat()

            val touchX = event.x - leftPadding
            if (touchX >= 0 && touchX <= chartWidth) {
                val index = (touchX / columnWidth).toInt().coerceIn(0, graphData.size - 1)
                selectedIndex = index
                invalidate()
                if (event.action == MotionEvent.ACTION_UP) {
                    onDaySelectedListener?.invoke(graphData[index])
                }
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
