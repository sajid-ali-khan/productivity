package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class ProductivityRepository(
    private val habitDao: HabitDao,
    private val studyDao: StudyDao,
    private val vocabDao: VocabDao,
    private val taskDao: TaskDao
) {

    // 0. Word of the Day & Vocabulary
    fun getWordOfTheDay(date: String): Flow<VocabWordEntity?> = vocabDao.getWordForDate(date)

    val vocabHistory: Flow<List<VocabWordEntity>> = vocabDao.getAllVocabHistory()

    val savedVocabWords: Flow<List<VocabWordEntity>> = vocabDao.getSavedWords()

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
    // All-Time Habit Score across all recorded days and active habits
    val allTimeHabitScore: Flow<Int> = combine(
        habitDao.getActiveHabits(),
        habitDao.getAllLogs()
    ) { activeHabits, logs ->
        if (activeHabits.isEmpty()) return@combine 0

        val todayStr = DateUtils.getTodayDateString()
        val allRecordedDates = (logs.map { it.date } + todayStr).distinct()
        val totalOpportunities = allRecordedDates.size * activeHabits.size
        val totalCompleted = logs.count { it.isCompleted }

        if (totalOpportunities > 0) {
            ((totalCompleted * 100) / totalOpportunities).coerceIn(0, 100)
        } else {
            0
        }
    }

    // Generates reports based on how many habits user followed and skipped
    val habitReports: Flow<List<HabitReportItem>> = combine(
        habitDao.getActiveHabits(),
        habitDao.getAllLogs()
    ) { habits, logs ->
        val logsByHabit = logs.groupBy { it.habitId }
        val todayStr = DateUtils.getTodayDateString()
        val totalRecordedDays = (logs.map { it.date } + todayStr).distinct().size

        habits.map { habit ->
            val habitLogs = logsByHabit[habit.id] ?: emptyList()
            val followedCount = habitLogs.count { it.isCompleted }
            val skippedCount = (totalRecordedDays - followedCount).coerceAtLeast(0)

            val ratePercent = if (totalRecordedDays > 0) {
                ((followedCount * 100) / totalRecordedDays).coerceIn(0, 100)
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
                totalDaysTracked = totalRecordedDays,
                completionRatePercent = ratePercent,
                currentStreak = streak
            )
        }
    }

    // Day-by-day logs history
    val habitHistoryByDate: Flow<List<DayHabitHistory>> = combine(
        habitDao.getActiveHabits(),
        habitDao.getAllLogs()
    ) { activeHabits, logs ->
        if (activeHabits.isEmpty()) {
            return@combine emptyList<DayHabitHistory>()
        }

        val todayStr = DateUtils.getTodayDateString()
        val allDates = (logs.map { it.date } + todayStr).distinct().sortedDescending()
        val logsByDate = logs.groupBy { it.date }

        allDates.map { date ->
            val dateLogsMap = (logsByDate[date] ?: emptyList()).associateBy { it.habitId }
            val followed = mutableListOf<String>()
            val skipped = mutableListOf<String>()

            activeHabits.forEach { habit ->
                val log = dateLogsMap[habit.id]
                if (log?.isCompleted == true) {
                    followed.add(habit.name)
                } else {
                    skipped.add(habit.name)
                }
            }

            DayHabitHistory(
                date = date,
                displayDate = DateUtils.formatDisplayDate(date),
                totalHabits = activeHabits.size,
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

    suspend fun getStudySessionByDateAndSubject(date: String, subject: String): StudySessionEntity? {
        val cleanSubject = subject.trim().ifBlank { "General" }
        return studyDao.getSessionByDateAndSubject(date, cleanSubject)
    }

    suspend fun saveStudySession(
        date: String,
        startTime: Long,
        endTime: Long,
        durationSeconds: Long,
        subject: String = "General"
    ): Long {
        val cleanSubject = subject.trim().ifBlank { "General" }
        val existing = studyDao.getSessionByDateAndSubject(date, cleanSubject)
        return if (existing != null) {
            studyDao.updateSession(
                existing.copy(
                    durationSeconds = durationSeconds,
                    endTime = endTime,
                    subject = cleanSubject
                )
            )
            existing.id
        } else {
            val session = StudySessionEntity(
                date = date,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = durationSeconds,
                subject = cleanSubject
            )
            studyDao.insertSession(session)
        }
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

    // 4. Tasks & Todo Management (Google Tasks Style)
    val allTaskLists: Flow<List<TaskListEntity>> = taskDao.getAllLists()

    val starredTasks: Flow<List<TaskEntity>> = taskDao.getStarredTasks()

    fun getTasksForList(listId: Long): Flow<List<TaskEntity>> = taskDao.getTasksForList(listId)

    suspend fun addTask(
        listId: Long,
        title: String,
        notes: String = "",
        isStarred: Boolean = false
    ): Long {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) return -1L
        val task = TaskEntity(
            listId = listId,
            title = cleanTitle,
            notes = notes.trim(),
            isCompleted = false,
            isStarred = isStarred,
            createdAt = System.currentTimeMillis()
        )
        return taskDao.insertTask(task)
    }

    suspend fun setTaskCompletion(task: TaskEntity, isCompleted: Boolean) {
        val updated = task.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) System.currentTimeMillis() else null
        )
        taskDao.updateTask(updated)
    }

    suspend fun setTaskStarred(task: TaskEntity, isStarred: Boolean) {
        val updated = task.copy(isStarred = isStarred)
        taskDao.updateTask(updated)
    }

    suspend fun updateTask(task: TaskEntity) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(taskId: Long) {
        taskDao.deleteTaskById(taskId)
    }

    suspend fun deleteCompletedTasksForList(listId: Long) {
        taskDao.deleteCompletedTasksForList(listId)
    }

    suspend fun addTaskList(name: String): Long {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return -1L
        return taskDao.insertList(
            TaskListEntity(
                name = cleanName,
                isDefault = false
            )
        )
    }

    suspend fun renameTaskList(listId: Long, newName: String) {
        val cleanName = newName.trim()
        if (cleanName.isBlank()) return
        val existing = taskDao.getListById(listId) ?: return
        taskDao.updateList(existing.copy(name = cleanName))
    }

    suspend fun deleteTaskList(listId: Long) {
        taskDao.deleteListAndItsTasks(listId)
    }
}
