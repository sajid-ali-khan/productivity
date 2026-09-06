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
import com.example.data.DayStudyGraphData

class StudyTimeGraphView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var graphData: List<DayStudyGraphData> = emptyList()
    private var selectedIndex: Int = -1
    private var dailyGoalSeconds: Long = 7200L // 2 hours

    var onDaySelectedListener: ((DayStudyGraphData) -> Unit)? = null

    // Colors resolved dynamically
    private val colorGrid get() = ContextCompat.getColor(context, R.color.app_stroke)
    private val colorGoalLine get() = ContextCompat.getColor(context, R.color.app_text_muted)
    private val colorTextMuted get() = ContextCompat.getColor(context, R.color.app_text_muted)
    private val colorTextPrimary get() = ContextCompat.getColor(context, R.color.app_text_primary)
    private val colorTextSecondary get() = ContextCompat.getColor(context, R.color.app_text_secondary)
    private val colorTrack get() = ContextCompat.getColor(context, R.color.app_surface_variant)
    private val colorBarToday get() = ContextCompat.getColor(context, R.color.app_text_primary)
    private val colorBarPast get() = ContextCompat.getColor(context, R.color.app_text_secondary)
    private val colorHighlightBorder get() = ContextCompat.getColor(context, R.color.app_text_primary)
    private val colorBadgeBg get() = ContextCompat.getColor(context, R.color.app_surface_variant)

    // Paints
    private val goalGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 2f
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(12f, 10f), 0f)
    }

    private val baseGridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1.5f
        style = Paint.Style.STROKE
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(10.5f)
        textAlign = Paint.Align.CENTER
    }

    private val goalLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(9.5f)
        textAlign = Paint.Align.LEFT
    }

    private val badgeBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val badgeTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(9f)
        textAlign = Paint.Align.CENTER
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
    private val badgeRect = RectF()

    fun setData(data: List<DayStudyGraphData>, goalSeconds: Long = 7200L) {
        this.graphData = data
        this.dailyGoalSeconds = goalSeconds
        val todayIdx = data.indexOfFirst { it.isToday }
        if (todayIdx >= 0) {
            selectedIndex = todayIdx
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        goalGridPaint.color = colorGoalLine
        baseGridPaint.color = colorGrid
        goalLabelPaint.color = colorTextMuted
        badgeBgPaint.color = colorBadgeBg
        badgeTextPaint.color = colorTextSecondary
        trackPaint.color = colorTrack
        highlightStrokePaint.color = colorHighlightBorder

        if (graphData.isEmpty()) {
            val emptyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = colorTextMuted
                textSize = dpToPx(13f)
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("No study sessions recorded yet", width / 2f, height / 2f, emptyPaint)
            return
        }

        val topPadding = dpToPx(44f)
        val bottomPadding = dpToPx(30f)
        val leftPadding = dpToPx(38f)
        val rightPadding = dpToPx(16f)

        val chartWidth = width - leftPadding - rightPadding
        val chartHeight = height - topPadding - bottomPadding

        if (chartHeight <= 0 || chartWidth <= 0) return

        // Compute scale: maximum of (dailyGoalSeconds, max single day + 15%)
        val maxSecondsInWeek = graphData.maxOfOrNull { it.totalSeconds } ?: 0L
        val maxScale = (maxOf(dailyGoalSeconds, maxSecondsInWeek) * 1.15f).coerceAtLeast(3600f)

        // Draw Baseline (0s)
        val baselineY = topPadding + chartHeight
        canvas.drawLine(leftPadding, baselineY, width - rightPadding, baselineY, baseGridPaint)
        canvas.drawText("0m", dpToPx(4f), baselineY + dpToPx(4f), goalLabelPaint)

        // Draw Daily Goal line
        val goalRatio = (dailyGoalSeconds.toFloat() / maxScale).coerceIn(0f, 1f)
        val goalY = topPadding + chartHeight * (1f - goalRatio)
        canvas.drawLine(leftPadding, goalY, width - rightPadding, goalY, goalGridPaint)
        val goalHours = dailyGoalSeconds / 3600
        canvas.drawText("${goalHours}h Goal", dpToPx(4f), goalY + dpToPx(3f), goalLabelPaint)

        // Draw 7 day bars
        val itemCount = graphData.size
        val columnWidth = chartWidth / itemCount.toFloat()
        val barWidth = dpToPx(24f).coerceAtMost(columnWidth * 0.65f)
        val cornerRadius = barWidth / 2f

        for (i in 0 until itemCount) {
            val item = graphData[i]
            val centerX = leftPadding + (i + 0.5f) * columnWidth

            // Track (full height background)
            trackRect.set(
                centerX - barWidth / 2f,
                topPadding,
                centerX + barWidth / 2f,
                baselineY
            )
            canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, trackPaint)

            // Filled bar (proportional to total seconds)
            val fillRatio = (item.totalSeconds.toFloat() / maxScale).coerceIn(0f, 1f)
            val fillHeight = fillRatio * chartHeight
            if (fillHeight > 0f) {
                barRect.set(
                    centerX - barWidth / 2f,
                    baselineY - fillHeight,
                    centerX + barWidth / 2f,
                    baselineY
                )
                barPaint.color = if (item.isToday) colorBarToday else colorBarPast
                canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint)
            }

            // Selection indicator or today ring
            if (i == selectedIndex) {
                canvas.drawRoundRect(trackRect, cornerRadius, cornerRadius, highlightStrokePaint)
            }

            // Duration text on top of bar
            textPaint.color = if (item.isToday) colorTextPrimary else colorTextMuted
            textPaint.isFakeBoldText = item.isToday || i == selectedIndex
            val durationText = item.formattedDuration
            canvas.drawText(durationText, centerX, topPadding - dpToPx(22f), textPaint)

            // Session count tag above bar if sessionCount > 0
            if (item.sessionCount > 0) {
                val badgeText = "${item.sessionCount} ses"
                val badgeW = dpToPx(32f)
                val badgeH = dpToPx(14f)
                badgeRect.set(
                    centerX - badgeW / 2f,
                    topPadding - dpToPx(18f),
                    centerX + badgeW / 2f,
                    topPadding - dpToPx(4f)
                )
                canvas.drawRoundRect(badgeRect, dpToPx(7f), dpToPx(7f), badgeBgPaint)
                canvas.drawText(badgeText, centerX, topPadding - dpToPx(7f), badgeTextPaint)
            }

            // Day label below bar
            val dayText = if (item.isToday) "Today" else item.dayLabel
            canvas.drawText(dayText, centerX, height - dpToPx(8f), textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            if (graphData.isEmpty()) return super.onTouchEvent(event)

            val leftPadding = dpToPx(38f)
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
