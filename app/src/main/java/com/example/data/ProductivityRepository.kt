package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProductivityRepository(
    private val habitDao: HabitDao,
    private val studyDao: StudyDao,
    private val vocabDao: VocabDao
) {

    // 0. Word of the Day & Vocabulary
    fun getWordOfTheDay(date: String): Flow<VocabWordEntity?> = vocabDao.getWordForDate(date)

    val vocabHistory: Flow<List<VocabWordEntity>> = vocabDao.getAllVocabHistory()

    suspend fun ensureWordOfTheDay(date: String) {
        val existing = vocabDao.getWordForDateSync(date)
        if (existing == null) {
            val curated = VocabularyCatalog.getWordForDate(date)
            val entity = VocabWordEntity(
                date = date,
                word = curated.word,
                phonetic = curated.phonetic,
                partOfSpeech = curated.partOfSpeech,
                definition = curated.definition,
                example = curated.example,
                speakingTip = curated.speakingTip,
                sampleDialogue = curated.sampleDialogue,
                audioUrl = null,
                isSaved = false
            )
            vocabDao.insertWord(entity)

            // Try network enrichment in background for native pronunciation audio
            try {
                val audioUrl = VocabularyCatalog.fetchApiEnrichment(curated.word)
                if (!audioUrl.isNullOrBlank()) {
                    vocabDao.insertWord(entity.copy(audioUrl = audioUrl))
                }
            } catch (_: Exception) {}
        }
    }

    suspend fun toggleSaveWord(date: String, isSaved: Boolean) {
        vocabDao.updateSaved(date, isSaved)
    }

    // 1. Habit Management
    val activeHabits: Flow<List<HabitEntity>> = habitDao.getActiveHabits()

    fun getHabitsWithStatusForDate(date: String): Flow<List<HabitWithStatus>> {
        return combine(habitDao.getActiveHabits(), habitDao.getLogsForDate(date)) { habits, logs ->
            val logMap = logs.associateBy { it.habitId }
            habits.map { habit ->
                val log = logMap[habit.id]
                HabitWithStatus(
                    habit = habit,
                    isCompleted = log?.isCompleted == true
                )
            }
        }
    }

    suspend fun addHabit(name: String): Long {
        return habitDao.insertHabit(HabitEntity(name = name.trim()))
    }

    suspend fun setHabitCompletion(habitId: Long, date: String, isCompleted: Boolean) {
        val log = HabitLogEntity(
            habitId = habitId,
            date = date,
            isCompleted = isCompleted,
            timestamp = System.currentTimeMillis()
        )
        habitDao.insertOrUpdateLog(log)
    }

    suspend fun deleteHabit(habitId: Long) {
        habitDao.deleteHabitAndLogs(habitId)
    }

    // 2. Habit Reports & History
    // Generates reports based on how many habits user followed and skipped
    val habitReports: Flow<List<HabitReportItem>> = combine(
        habitDao.getAllHabits(),
        habitDao.getAllLogs()
    ) { habits, logs ->
        val logsByHabit = logs.groupBy { it.habitId }

        habits.map { habit ->
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val followedCount = habitLogs.count { it.isCompleted }
            val skippedCount = habitLogs.count { !it.isCompleted }
            val totalTracked = followedCount + skippedCount

            val ratePercent = if (totalTracked > 0) {
                (followedCount * 100) / totalTracked
            } else {
                0
            }

            // Calculate current streak (consecutive completed days ending today/yesterday)
            val sortedLogs = habitLogs.sortedByDescending { it.date }
            var streak = 0
            for (log in sortedLogs) {
                if (log.isCompleted) {
                    streak++
                } else {
                    break
                }
            }

            HabitReportItem(
                habitId = habit.id,
                name = habit.name,
                followedDays = followedCount,
                skippedDays = skippedCount,
                totalDaysTracked = totalTracked,
                completionRatePercent = ratePercent,
                currentStreak = streak
            )
        }
    }

    // Day-by-day logs history
    val habitHistoryByDate: Flow<List<DayHabitHistory>> = combine(
        habitDao.getAllHabits(),
        habitDao.getAllLogs()
    ) { habits, logs ->
        val habitNameMap = habits.associate { it.id to it.name }
        val logsByDate = logs.groupBy { it.date }.toSortedMap(compareByDescending { it })

        logsByDate.map { (date, dateLogs) ->
            val followed = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            dateLogs.forEach { log ->
                val name = habitNameMap[log.habitId] ?: "Habit #${log.habitId}"
                if (log.isCompleted) {
                    followed.add(name)
                } else {
                    skipped.add(name)
                }
            }

            DayHabitHistory(
                date = date,
                displayDate = DateUtils.formatDisplayDate(date),
                totalHabits = dateLogs.size,
                completedCount = followed.size,
                followedHabits = followed,
                skippedHabits = skipped
            )
        }
    }

    // 3. Study Timer & History
    val allStudySessions: Flow<List<StudySessionEntity>> = studyDao.getAllSessions()

    fun getTodayStudySeconds(date: String): Flow<Long> = studyDao.getTotalDurationForDate(date)

    fun getSessionsForDate(date: String): Flow<List<StudySessionEntity>> = studyDao.getSessionsForDate(date)

    suspend fun deleteStudySession(id: Long) = studyDao.deleteSessionById(id)

    suspend fun saveStudySession(
        date: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        subject: String = "General"
    ): Long {
        val cleanSubject = subject.trim().ifBlank { "General" }
        val session = StudySessionEntity(
            date = date,
            startTime = startTime,
            endTime = endTime,
            durationSeconds = durationSeconds,
            subject = cleanSubject
        )
        return studyDao.insertSession(session)
    }

    suspend fun updateStudySession(
        id: Long,
        durationSeconds: Long,
        endTime: Long,
        subject: String = "General"
    ) {
        val existing = studyDao.getSessionById(id) ?: return
        val cleanSubject = subject.trim().ifBlank { "General" }
        studyDao.updateSession(
            existing.copy(
                durationSeconds = durationSeconds,
                endTime = endTime,
                subject = cleanSubject
            )
        )
    }

    suspend fun getStudySessionById(id: Long): StudySessionEntity? {
        return studyDao.getSessionById(id)
    }

    // 7-Day Habit Completion Graph Data
    val last7DaysHabitsGraph: Flow<List<DayHabitGraphData>> = combine(
        habitDao.getActiveHabits(),
        habitDao.getAllLogs()
    ) { habits, logs ->
        val last7Days = DateUtils.getLast7Days()
        val todayStr = DateUtils.getTodayDateString()
        val logsByDate = logs.groupBy { it.date }
        val activeCount = habits.size

        last7Days.map { dateStr ->
            val dayLogs = logsByDate[dateStr] ?: emptyList()
            val completed = dayLogs.count { it.isCompleted }
            val total = if (activeCount > 0) activeCount else dayLogs.size
            val rate = if (total > 0) ((completed * 100) / total).coerceIn(0, 100) else 0

            DayHabitGraphData(
                date = dateStr,
                dayLabel = DateUtils.getDayOfWeekLabel(dateStr),
                completedCount = completed,
                totalCount = total,
                completionRatePercent = rate,
                isToday = dateStr == todayStr
            )
        }
    }

    // 7-Day Study Time Graph Data
    val last7DaysStudyGraph: Flow<List<DayStudyGraphData>> = studyDao.getAllSessions().map { sessions ->
        val last7Days = DateUtils.getLast7Days()
        val todayStr = DateUtils.getTodayDateString()
        val sessionsByDate = sessions.groupBy { it.date }

        last7Days.map { dateStr ->
            val daySessions = sessionsByDate[dateStr] ?: emptyList()
            val totalSeconds = daySessions.sumOf { it.durationSeconds }

            DayStudyGraphData(
                date = dateStr,
                dayLabel = DateUtils.getDayOfWeekLabel(dateStr),
                totalSeconds = totalSeconds,
                sessionCount = daySessions.size,
                isToday = dateStr == todayStr
            )
        }
    }

    // Step-counter style daily study history with separate sessions (sorted by length descending)
    val dailyStudyHistoryWithSessions: Flow<List<DailyStudyHistoryWithSessions>> = studyDao.getAllSessions().map { sessions ->
        val groupedByDate = sessions.groupBy { it.date }.toSortedMap(compareByDescending { it })

        groupedByDate.map { (date, daySessions) ->
            val totalSec = daySessions.sumOf { it.durationSeconds }
            DailyStudyHistoryWithSessions(
                date = date,
                displayDate = DateUtils.formatHeaderDate(date),
                totalSeconds = totalSec,
                sessionCount = daySessions.size,
                sessions = daySessions.sortedByDescending { it.durationSeconds }
            )
        }
    }

    // Step-counter style daily study history
    val dailyStudyHistory: Flow<List<DailyStudyHistory>> = studyDao.getAllSessions().map { sessions ->
        val groupedByDate = sessions.groupBy { it.date }.toSortedMap(compareByDescending { it })

        groupedByDate.map { (date, daySessions) ->
            val totalSec = daySessions.sumOf { it.durationSeconds }
            DailyStudyHistory(
                date = date,
                displayDate = DateUtils.formatDisplayDate(date),
                totalSeconds = totalSec,
                sessionCount = daySessions.size
            )
        }
    }
}
