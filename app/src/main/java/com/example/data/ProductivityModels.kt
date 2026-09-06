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

object DateUtils {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
    private val dayOfWeekFormat = SimpleDateFormat("EEE", Locale.getDefault())
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
