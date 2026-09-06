package com.example

import com.example.data.DailyStudyHistory
import com.example.data.DailyStudyHistoryWithSessions
import com.example.data.DateUtils
import com.example.data.DayStudyGraphData
import com.example.data.StudySessionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun testDateUtils_formatSecondsToClock() {
    assertEquals("00:00", DateUtils.formatSecondsToClock(0))
    assertEquals("01:30", DateUtils.formatSecondsToClock(90))
    assertEquals("01:00:00", DateUtils.formatSecondsToClock(3600))
    assertEquals("01:15:30", DateUtils.formatSecondsToClock(4530))
  }

  @Test
  fun testDateUtils_getLast7Days() {
    val days = DateUtils.getLast7Days()
    assertEquals(7, days.size)
    assertEquals(DateUtils.getTodayDateString(), days.last())
  }

  @Test
  fun testDateUtils_formatDurationCompact() {
    assertEquals("45s", DateUtils.formatDurationCompact(45))
    assertEquals("15m 30s", DateUtils.formatDurationCompact(930))
    assertEquals("1h 30m", DateUtils.formatDurationCompact(5400))
  }

  @Test
  fun testDateUtils_formatHeaderDate_noDuplicateToday() {
    val today = DateUtils.getTodayDateString()
    val header = DateUtils.formatHeaderDate(today)
    assertTrue("Header should start with Today", header.startsWith("Today •"))
    org.junit.Assert.assertFalse("Header should not contain duplicate Today", header.contains("Today • Today"))
  }

  @Test
  fun testDayStudyGraphData_formattedDuration() {
    val graphItem = DayStudyGraphData(
      date = "2026-09-06",
      dayLabel = "Today",
      totalSeconds = 5400L,
      sessionCount = 2,
      isToday = true
    )
    assertEquals("1h 30m", graphItem.formattedDuration)
  }

  @Test
  fun testDailyStudyHistoryWithSessions() {
    val sessions = listOf(
      StudySessionEntity(id = 1, date = "2026-09-06", startTime = 1000L, endTime = 2800L, durationSeconds = 1800L, subject = "Math"),
      StudySessionEntity(id = 2, date = "2026-09-06", startTime = 3000L, endTime = 4800L, durationSeconds = 1800L, subject = "Physics")
    )
    val history = DailyStudyHistoryWithSessions(
      date = "2026-09-06",
      displayDate = "Today",
      totalSeconds = 3600L,
      sessionCount = 2,
      sessions = sessions,
      goalSeconds = 7200L
    )
    assertEquals("1h 0m", history.formattedDuration)
    assertEquals(50, history.progressPercent)
    assertEquals(2, history.sessions.size)
  }
}

