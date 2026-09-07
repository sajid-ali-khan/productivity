package com.example.ui.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.DateUtils
import com.example.data.HeatmapDayCell
import java.text.SimpleDateFormat
import java.util.Locale

class HeatmapCalendarView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    enum class Mode {
        HABIT,
        STUDY
    }

    private var mode: Mode = Mode.HABIT
    private val numWeeks = 14
    private var cells: List<HeatmapDayCell> = emptyList()
    private var selectedCell: HeatmapDayCell? = null

    var onCellSelectedListener: ((HeatmapDayCell) -> Unit)? = null

    // Colors
    private val colorStroke get() = ContextCompat.getColor(context, R.color.app_stroke)
    private val colorSurfaceVariant get() = ContextCompat.getColor(context, R.color.app_surface_variant)
    private val colorTextMuted get() = ContextCompat.getColor(context, R.color.app_text_muted)
    private val colorTextPrimary get() = ContextCompat.getColor(context, R.color.app_text_primary)

    // Habit Colors
    private val colorHabitCompleted = 0xFF22C55E.toInt() // Green
    private val colorHabitMissed get() = colorSurfaceVariant

    // Study Intensity Shades (Green gradient)
    private val colorStudyLevel0 get() = colorSurfaceVariant
    private val colorStudyLevel1 = 0x5522C55E.toInt() // 33%
    private val colorStudyLevel2 = 0x8822C55E.toInt() // 53%
    private val colorStudyLevel3 = 0xBB22C55E.toInt() // 73%
    private val colorStudyLevel4 = 0xFF22C55E.toInt() // 100%

    // Paints
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }

    private val cellStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    private val highlightStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = colorTextPrimary
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = dpToPx(9.5f)
        textAlign = Paint.Align.LEFT
    }

    private val cellRect = RectF()
    private val cellCornerRadius = dpToPx(3.5f)

    init {
        loadBaseGrid()
    }

    private fun loadBaseGrid() {
        cells = DateUtils.getHeatmapGridDays(numWeeks)
    }

    fun setHabitData(completedDates: Set<String>, allTrackedDates: Set<String>) {
        mode = Mode.HABIT
        val baseGrid = DateUtils.getHeatmapGridDays(numWeeks)
        cells = baseGrid.map { cell ->
            val isDone = completedDates.contains(cell.dateStr)
            cell.copy(
                isCompleted = isDone,
                intensityLevel = if (isDone) 4 else 0
            )
        }
        selectedCell = cells.find { it.isToday }
        invalidate()
    }

    fun setStudyData(dailyDurations: Map<String, Long>) {
        mode = Mode.STUDY
        val baseGrid = DateUtils.getHeatmapGridDays(numWeeks)
        cells = baseGrid.map { cell ->
            val duration = dailyDurations[cell.dateStr] ?: 0L
            val level = when {
                duration <= 0L -> 0
                duration < 1800L -> 1 // < 30m
                duration < 3600L -> 2 // 30m - 1h
                duration < 7200L -> 3 // 1h - 2h
                else -> 4 // 2h+
            }
            cell.copy(
                durationSeconds = duration,
                isCompleted = duration > 0,
                intensityLevel = level
            )
        }
        selectedCell = cells.find { it.isToday }
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        // 7 rows + 1 row for month labels + margins
        val estimatedHeight = dpToPx(160f).toInt()
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val finalHeight = if (heightMode == MeasureSpec.EXACTLY) {
            MeasureSpec.getSize(heightMeasureSpec)
        } else {
            estimatedHeight
        }
        setMeasuredDimension(width, finalHeight)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cells.isEmpty()) return

        val paddingL = paddingLeft.toFloat() + dpToPx(22f) // space for day labels (M, W, F)
        val paddingT = paddingTop.toFloat() + dpToPx(18f) // space for month labels
        val availableW = width - paddingL - paddingRight.toFloat()
        val availableH = height - paddingT - paddingBottom.toFloat()

        val numCols = numWeeks
        val numRows = 7

        val cellGap = dpToPx(3.5f)
        val cellSize = minOf(
            (availableW - (numCols - 1) * cellGap) / numCols,
            (availableH - (numRows - 1) * cellGap) / numRows
        ).coerceAtLeast(dpToPx(10f))

        // Draw Day of Week Labels (Mon, Wed, Fri) on the left
        textPaint.color = colorTextMuted
        textPaint.textAlign = Paint.Align.RIGHT
        val labelX = paddingL - dpToPx(6f)
        val dayLabels = mapOf(0 to "M", 2 to "W", 4 to "F")
        dayLabels.forEach { (row, label) ->
            val y = paddingT + row * (cellSize + cellGap) + cellSize * 0.75f
            canvas.drawText(label, labelX, y, textPaint)
        }

        // Draw Month Labels on the top
        textPaint.textAlign = Paint.Align.LEFT
        var lastMonth = ""
        for (w in 0 until numCols) {
            val weekCells = cells.filter { it.weekIndex == w }
            val firstCellOfWeek = weekCells.firstOrNull()
            if (firstCellOfWeek != null && firstCellOfWeek.monthName != lastMonth) {
                lastMonth = firstCellOfWeek.monthName
                val x = paddingL + w * (cellSize + cellGap)
                val y = paddingT - dpToPx(6f)
                canvas.drawText(lastMonth, x, y, textPaint)
            }
        }

        // Draw Heatmap Cells
        for (cell in cells) {
            if (cell.isFuture) continue // Don't draw future dates

            val x = paddingL + cell.weekIndex * (cellSize + cellGap)
            val y = paddingT + cell.dayOfWeek * (cellSize + cellGap)
            cellRect.set(x, y, x + cellSize, y + cellSize)

            // Determine Fill Color
            if (mode == Mode.HABIT) {
                if (cell.isCompleted) {
                    cellPaint.color = colorHabitCompleted
                    canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, cellPaint)
                } else {
                    cellPaint.color = colorHabitMissed
                    canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, cellPaint)
                    cellStrokePaint.color = colorStroke
                    canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, cellStrokePaint)
                }
            } else {
                // Study Mode
                val color = when (cell.intensityLevel) {
                    1 -> colorStudyLevel1
                    2 -> colorStudyLevel2
                    3 -> colorStudyLevel3
                    4 -> colorStudyLevel4
                    else -> colorStudyLevel0
                }
                cellPaint.color = color
                canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, cellPaint)
                if (cell.intensityLevel == 0) {
                    cellStrokePaint.color = colorStroke
                    canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, cellStrokePaint)
                }
            }

            // Highlight Today with a subtle dot or outline
            if (cell.isToday) {
                highlightStrokePaint.color = colorTextPrimary
                canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, highlightStrokePaint)
            }

            // Highlight User Selection
            if (selectedCell != null && selectedCell?.dateStr == cell.dateStr) {
                highlightStrokePaint.color = colorTextPrimary
                canvas.drawRoundRect(cellRect, cellCornerRadius, cellCornerRadius, highlightStrokePaint)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN || event.action == MotionEvent.ACTION_UP) {
            val paddingL = paddingLeft.toFloat() + dpToPx(22f)
            val paddingT = paddingTop.toFloat() + dpToPx(18f)
            val availableW = width - paddingL - paddingRight.toFloat()
            val availableH = height - paddingT - paddingBottom.toFloat()

            val numCols = numWeeks
            val numRows = 7
            val cellGap = dpToPx(3.5f)
            val cellSize = minOf(
                (availableW - (numCols - 1) * cellGap) / numCols,
                (availableH - (numRows - 1) * cellGap) / numRows
            ).coerceAtLeast(dpToPx(10f))

            val touchX = event.x - paddingL
            val touchY = event.y - paddingT

            val col = (touchX / (cellSize + cellGap)).toInt()
            val row = (touchY / (cellSize + cellGap)).toInt()

            if (col in 0 until numCols && row in 0 until numRows) {
                val tapped = cells.find { it.weekIndex == col && it.dayOfWeek == row && !it.isFuture }
                if (tapped != null) {
                    selectedCell = tapped
                    invalidate()
                    if (event.action == MotionEvent.ACTION_UP) {
                        onCellSelectedListener?.invoke(tapped)
                        performClick()
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun performClick(): Boolean {
        super.performClick()
        return true
    }

    private fun dpToPx(dp: Float): Float = dp * resources.displayMetrics.density
}
