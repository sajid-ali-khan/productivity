package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class HabitWithStatus(
    val habit: HabitEntity,
    val isCompleted: Boolean
)

data class HabitReportItem(
    val habitId: Long,
    val name: String,
    val followedDays: Int,
    val skippedDays: Int,
    val totalDaysTracked: Int,
    val completionRatePercent: Int,
    val currentStreak: Int
)

data class DayHabitHistory(
    val date: String,
    val displayDate: String,
    val totalHabits: Int,
    val completedCount: Int,
    val followedHabits: List<String>,
    val skippedHabits: List<String>
)

data class DailyStudyHistory(
    val date: String,
    val displayDate: String,
    val totalSeconds: Long,
    val sessionCount: Int,
    val goalSeconds: Long = 7200L // 2 hours default
) {
    val formattedDuration: String
        get() {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }

    val progressPercent: Int
        get() {
            if (goalSeconds <= 0) return 0
            val percent = (totalSeconds.toDouble() / goalSeconds.toDouble() * 100).toInt()
            return percent.coerceIn(0, 100)
        }
}

data class DayHabitGraphData(
    val date: String,
    val dayLabel: String,
    val completedCount: Int,
    val totalCount: Int,
    val completionRatePercent: Int,
    val isToday: Boolean
)

data class DayStudyGraphData(
    val date: String,
    val dayLabel: String,
    val totalSeconds: Long,
    val sessionCount: Int,
    val isToday: Boolean,
    val goalSeconds: Long = 7200L
) {
    val formattedDuration: String
        get() {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return when {
                hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
                hours > 0 -> "${hours}h"
                minutes > 0 -> "${minutes}m"
                totalSeconds > 0 -> "${totalSeconds}s"
                else -> "0m"
            }
        }
}

data class DailyStudyHistoryWithSessions(
    val date: String,
    val displayDate: String,
    val totalSeconds: Long,
    val sessionCount: Int,
    val sessions: List<StudySessionEntity>,
    val goalSeconds: Long = 7200L
) {
    val formattedDuration: String
        get() {
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            return if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        }

    val progressPercent: Int
        get() {
            if (goalSeconds <= 0) return 0
            val percent = (totalSeconds.toDouble() / goalSeconds.toDouble() * 100).toInt()
            return percent.coerceIn(0, 100)
        }
}

data class HabitHeatmapData(
    val habitId: Long,
    val habitName: String,
    val currentStreak: Int,
    val bestStreak: Int,
    val totalDaysTracked: Int,
    val followedDays: Int,
    val completionRatePercent: Int,
    val completedDates: Set<String>,
    val allTrackedDates: Set<String>
)

data class StudyHeatmapData(
    val totalSeconds: Long,
    val totalSessions: Int,
    val activeDaysCount: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val dailyDurations: Map<String, Long>,
    val subjectSummary: List<SubjectStudySummary> = emptyList()
)

data class SubjectStudySummary(
    val subject: String,
    val totalSeconds: Long,
    val sessionCount: Int
)

data class HeatmapDayCell(
    val dateStr: String,
    val dayOfWeek: Int, // 1 (Mon) to 7 (Sun)
    val weekIndex: Int,
    val monthName: String,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isCompleted: Boolean = false,
    val durationSeconds: Long = 0L,
    val intensityLevel: Int = 0 // 0 to 4
)

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private val monthFormat = SimpleDateFormat("MMM", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

    fun getTodayDateString(): String = dateFormat.format(Date())

    fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return dateFormat.format(cal.time)
    }

    fun getLast7Days(): List<String> {
        val list = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -6)
        for (i in 0 until 7) {
            list.add(dateFormat.format(cal.time))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }
        return list
    }

    /**
     * Generates a grid of days for a multi-week contribution heatmap (Monday = row 0 .. Sunday = row 6).
     */
    fun getHeatmapGridDays(numWeeks: Int = 12): List<HeatmapDayCell> {
        val cells = mutableListOf<HeatmapDayCell>()
        val todayStr = getTodayDateString()
        val todayCal = Calendar.getInstance()

        // End on the upcoming Sunday of the current week (or today)
        val endCal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            val dayOfWeek = get(Calendar.DAY_OF_WEEK)
            val daysToSunday = if (dayOfWeek == Calendar.SUNDAY) 0 else (Calendar.SUNDAY + 7 - dayOfWeek) % 7
            add(Calendar.DAY_OF_YEAR, daysToSunday)
        }

        // Start (numWeeks - 1) weeks prior on a Monday
        val startCal = (endCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, -(numWeeks * 7 - 1))
        }

        var currentCal = startCal.clone() as Calendar
        var currentWeek = 0
        var dayInWeek = 0

        while (!currentCal.after(endCal)) {
            val dateStr = dateFormat.format(currentCal.time)
            // Convert Calendar.DAY_OF_WEEK (Sun=1..Sat=7) to Mon=0..Sun=6
            val calDow = currentCal.get(Calendar.DAY_OF_WEEK)
            val monDow = if (calDow == Calendar.SUNDAY) 6 else (calDow - 2)
            val monthStr = monthFormat.format(currentCal.time)
            val isFuture = currentCal.after(todayCal) && dateStr != todayStr
            val isToday = dateStr == todayStr

            cells.add(
                HeatmapDayCell(
                    dateStr = dateStr,
                    dayOfWeek = monDow,
                    weekIndex = currentWeek,
                    monthName = monthStr,
                    isToday = isToday,
                    isFuture = isFuture
                )
            )

            currentCal.add(Calendar.DAY_OF_YEAR, 1)
            dayInWeek++
            if (dayInWeek % 7 == 0) {
                currentWeek++
            }
        }

        return cells
    }

    fun getDayOfWeekLabel(dateStr: String): String {
        return try {
            val today = getTodayDateString()
            if (dateStr == today) return "Today"
            val parsed = dateFormat.parse(dateStr)
            if (parsed != null) dayOfWeekFormat.format(parsed) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatDisplayDate(dateStr: String): String {
        return try {
            val today = getTodayDateString()
            val yesterday = getYesterdayDateString()
            when (dateStr) {
                today -> "Today"
                yesterday -> "Yesterday"
                else -> {
                    val parsed = dateFormat.parse(dateStr)
                    if (parsed != null) displayFormat.format(parsed) else dateStr
                }
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatHeaderDate(dateStr: String): String {
        return try {
            val today = getTodayDateString()
            val yesterday = getYesterdayDateString()
            val parsed = dateFormat.parse(dateStr)
            val formattedDate = if (parsed != null) displayFormat.format(parsed) else dateStr
            when (dateStr) {
                today -> "Today • $formattedDate"
                yesterday -> "Yesterday • $formattedDate"
                else -> formattedDate
            }
        } catch (e: Exception) {
            dateStr
        }
    }

    fun formatTimeRange(startTime: Long, endTime: Long): String {
        if (startTime <= 0) return ""
        val startStr = timeFormat.format(Date(startTime))
        val endStr = if (endTime > startTime) timeFormat.format(Date(endTime)) else ""
        return if (endStr.isNotEmpty()) "$startStr – $endStr" else startStr
    }

    fun formatDurationCompact(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes}m ${secs}s"
            else -> "${secs}s"
        }
    }

    fun formatSecondsToClock(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) {
            String.format(Locale.getDefault(), "%02d:%02d:%02d", h, m, s)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", m, s)
        }
    }
}
