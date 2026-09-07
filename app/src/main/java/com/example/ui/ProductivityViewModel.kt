package com.example.ui

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.DailyStudyHistory
import com.example.data.DailyStudyHistoryWithSessions
import com.example.data.DateUtils
import com.example.data.DayHabitGraphData
import com.example.data.DayHabitHistory
import com.example.data.DayStudyGraphData
import com.example.data.HabitHeatmapData
import com.example.data.HabitReportItem
import com.example.data.HabitWithStatus
import com.example.data.ProductivityRepository
import com.example.data.StudyHeatmapData
import com.example.data.StudySessionEntity
import com.example.data.TaskEntity
import com.example.data.TaskListEntity
import com.example.data.VocabWordEntity
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

enum class TaskSortOrder {
    MY_ORDER,
    STARRED_FIRST,
    ALPHABETICAL,
    NEWEST_FIRST
}

enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED
}

class ProductivityViewModel(
    private val repository: ProductivityRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(DateUtils.getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    // 0. Word of the Day & Vocabulary
    val wordOfTheDay: StateFlow<VocabWordEntity?> = _selectedDate
        .flatMapLatest { date ->
            repository.getWordOfTheDay(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val vocabHistory: StateFlow<List<VocabWordEntity>> = repository.vocabHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedVocabWords: StateFlow<List<VocabWordEntity>> = repository.savedVocabWords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        val today = DateUtils.getTodayDateString()
        viewModelScope.launch {
            repository.ensureWordOfTheDay(today)
            val existingGeneral = repository.getStudySessionByDateAndSubject(today, "General")
            if (existingGeneral != null && _timerState.value == TimerState.IDLE) {
                _activeSessionId.value = existingGeneral.id
                baseTimeElapsedMs = existingGeneral.durationSeconds * 1000L
                _elapsedSeconds.value = existingGeneral.durationSeconds
                sessionSystemStartTime = existingGeneral.startTime
            }
        }
    }

    fun toggleSaveWord(word: VocabWordEntity) {
        viewModelScope.launch {
            repository.toggleSaveWord(word.date, !word.isSaved)
        }
    }

    // 1. Today's Habits
    val habitsForDate: StateFlow<List<HabitWithStatus>> = _selectedDate
        .flatMapLatest { date ->
            repository.getHabitsWithStatusForDate(date)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addHabit(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repository.addHabit(name)
        }
    }

    fun toggleHabitCompletion(habitId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.setHabitCompletion(habitId, _selectedDate.value, isCompleted)
        }
    }

    fun renameHabit(habitId: Long, newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameHabit(habitId, newName)
        }
    }

    fun getHabitHeatmap(habitId: Long): Flow<HabitHeatmapData?> {
        return repository.getHabitHeatmapData(habitId)
    }

    val studyHeatmap: StateFlow<StudyHeatmapData> = repository.getStudyHeatmapData()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StudyHeatmapData(0L, 0, 0, 0, 0, emptyMap())
        )

    fun deleteHabit(habitId: Long) {
        viewModelScope.launch {
            repository.deleteHabit(habitId)
        }
    }

    // 2. Reports & History
    val allTimeHabitScore: StateFlow<Int> = repository.allTimeHabitScore
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val habitReports: StateFlow<List<HabitReportItem>> = repository.habitReports
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val habitHistory: StateFlow<List<DayHabitHistory>> = repository.habitHistoryByDate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyStudyHistory: StateFlow<List<DailyStudyHistory>> = repository.dailyStudyHistory
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val dailyStudyHistoryWithSessions: StateFlow<List<DailyStudyHistoryWithSessions>> = repository.dailyStudyHistoryWithSessions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val todayStudySeconds: StateFlow<Long> = repository.getTodayStudySeconds(DateUtils.getTodayDateString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val allTimeStudySeconds: StateFlow<Long> = repository.allStudySessions
        .map { sessions -> sessions.sumOf { it.durationSeconds } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    val todayStudySessions: StateFlow<List<StudySessionEntity>> = repository.getSessionsForDate(DateUtils.getTodayDateString())
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 7-Day Graphs Data
    val last7DaysHabitsGraph: StateFlow<List<DayHabitGraphData>> = repository.last7DaysHabitsGraph
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val last7DaysStudyGraph: StateFlow<List<DayStudyGraphData>> = repository.last7DaysStudyGraph
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun deleteStudySession(sessionId: Long) {
        viewModelScope.launch {
            if (_activeSessionId.value == sessionId) {
                timerJob?.cancel()
                resetTimer()
                _activeSessionId.value = null
                _currentSubject.value = "General"
            }
            repository.deleteStudySession(sessionId)
        }
    }

    // 3. Study Timer State Machine & Multi-Session Switcher
    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _currentSubject = MutableStateFlow("General")
    val currentSubject: StateFlow<String> = _currentSubject.asStateFlow()

    private val _timerState = MutableStateFlow(TimerState.IDLE)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private var timerJob: Job? = null
    private var baseTimeElapsedMs: Long = 0L
    private var timerStartTimeMs: Long = 0L
    private var sessionSystemStartTime: Long = 0L

    fun setSubject(subject: String) {
        val cleanSubject = subject.trim().ifBlank { "General" }
        _currentSubject.value = cleanSubject

        if (_timerState.value != TimerState.RUNNING) {
            viewModelScope.launch {
                val today = DateUtils.getTodayDateString()
                val existing = repository.getStudySessionByDateAndSubject(today, cleanSubject)
                if (existing != null) {
                    _activeSessionId.value = existing.id
                    baseTimeElapsedMs = existing.durationSeconds * 1000L
                    _elapsedSeconds.value = existing.durationSeconds
                    sessionSystemStartTime = existing.startTime
                } else {
                    _activeSessionId.value = null
                    baseTimeElapsedMs = 0L
                    _elapsedSeconds.value = 0L
                    sessionSystemStartTime = 0L
                }
            }
        }
    }

    fun startTimer(subject: String = "General") {
        if (_timerState.value == TimerState.IDLE) {
            val cleanSubject = subject.trim().ifBlank { _currentSubject.value.ifBlank { "General" } }
            _currentSubject.value = cleanSubject

            viewModelScope.launch {
                val today = DateUtils.getTodayDateString()
                val existing = repository.getStudySessionByDateAndSubject(today, cleanSubject)
                if (existing != null) {
                    _activeSessionId.value = existing.id
                    baseTimeElapsedMs = existing.durationSeconds * 1000L
                    _elapsedSeconds.value = existing.durationSeconds
                    sessionSystemStartTime = existing.startTime
                } else {
                    if (_activeSessionId.value == null) {
                        baseTimeElapsedMs = 0L
                        _elapsedSeconds.value = 0L
                        sessionSystemStartTime = System.currentTimeMillis()
                    }
                }
                timerStartTimeMs = SystemClock.elapsedRealtime()
                _timerState.value = TimerState.RUNNING
                startTicker()
            }
        }
    }

    fun pauseTimer() {
        if (_timerState.value == TimerState.RUNNING) {
            timerJob?.cancel()
            val chunk = SystemClock.elapsedRealtime() - timerStartTimeMs
            baseTimeElapsedMs += chunk
            _elapsedSeconds.value = baseTimeElapsedMs / 1000
            _timerState.value = TimerState.PAUSED
        }
    }

    fun resumeTimer() {
        if (_timerState.value == TimerState.PAUSED) {
            timerStartTimeMs = SystemClock.elapsedRealtime()
            _timerState.value = TimerState.RUNNING
            startTicker()
        }
    }

    /**
     * Switch to a saved session from today to resume studying on it.
     * "Only one session is active at a time."
     */
    fun switchToSession(session: StudySessionEntity, autoResume: Boolean = true) {
        viewModelScope.launch {
            // If already on this session, just toggle resume/pause
            if (_activeSessionId.value == session.id) {
                if (_timerState.value == TimerState.PAUSED && autoResume) {
                    resumeTimer()
                }
                return@launch
            }

            // Save progress of current active session before switching
            autoSaveCurrentActiveProgress()

            // Switch to requested session
            _activeSessionId.value = session.id
            val subject = session.subject.ifBlank { "General" }
            _currentSubject.value = subject
            baseTimeElapsedMs = session.durationSeconds * 1000L
            _elapsedSeconds.value = session.durationSeconds
            sessionSystemStartTime = session.startTime

            if (autoResume) {
                timerStartTimeMs = SystemClock.elapsedRealtime()
                _timerState.value = TimerState.RUNNING
                startTicker()
            } else {
                _timerState.value = TimerState.PAUSED
            }
        }
    }

    fun startNewSession(defaultSubject: String = "General") {
        viewModelScope.launch {
            autoSaveCurrentActiveProgress()
            resetTimer()
            _activeSessionId.value = null
            setSubject(defaultSubject)
        }
    }

    private fun startTicker() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                val currentChunk = SystemClock.elapsedRealtime() - timerStartTimeMs
                val totalMs = baseTimeElapsedMs + currentChunk
                _elapsedSeconds.value = totalMs / 1000
                delay(500)
            }
        }
    }

    private fun calculateCurrentDurationSeconds(): Long {
        return if (_timerState.value == TimerState.RUNNING) {
            val chunk = SystemClock.elapsedRealtime() - timerStartTimeMs
            (baseTimeElapsedMs + chunk) / 1000
        } else {
            baseTimeElapsedMs / 1000
        }
    }

    private suspend fun autoSaveCurrentActiveProgress() {
        if (_timerState.value == TimerState.IDLE) return
        timerJob?.cancel()

        val currentDuration = calculateCurrentDurationSeconds()
        val currentSub = _currentSubject.value.ifBlank { "General" }
        val activeId = _activeSessionId.value
        val today = DateUtils.getTodayDateString()

        if (activeId != null) {
            repository.updateStudySession(
                id = activeId,
                durationSeconds = currentDuration,
                endTime = System.currentTimeMillis(),
                subject = currentSub
            )
        } else if (currentDuration >= 5) {
            val endTime = System.currentTimeMillis()
            val startTime = if (sessionSystemStartTime > 0) sessionSystemStartTime else endTime - (currentDuration * 1000)
            repository.saveStudySession(
                date = today,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = currentDuration,
                subject = currentSub
            )
        }
    }

    suspend fun stopAndSaveTimer(subject: String): Boolean {
        timerJob?.cancel()
        val currentDuration = calculateCurrentDurationSeconds()
        val cleanSubject = subject.trim().ifBlank { _currentSubject.value.ifBlank { "General" } }
        val activeId = _activeSessionId.value
        val today = DateUtils.getTodayDateString()

        resetTimer()
        _activeSessionId.value = null
        _currentSubject.value = "General"

        val saved = if (activeId != null) {
            repository.updateStudySession(
                id = activeId,
                durationSeconds = currentDuration,
                endTime = System.currentTimeMillis(),
                subject = cleanSubject
            )
            true
        } else if (currentDuration >= 5) {
            val endTime = System.currentTimeMillis()
            val startTime = if (sessionSystemStartTime > 0) sessionSystemStartTime else endTime - (currentDuration * 1000)
            repository.saveStudySession(
                date = today,
                startTime = startTime,
                endTime = endTime,
                durationSeconds = currentDuration,
                subject = cleanSubject
            )
            true
        } else {
            false
        }

        // Check if today has an existing session for "General" to load for next start
        val existingGeneral = repository.getStudySessionByDateAndSubject(today, "General")
        if (existingGeneral != null) {
            _activeSessionId.value = existingGeneral.id
            baseTimeElapsedMs = existingGeneral.durationSeconds * 1000L
            _elapsedSeconds.value = existingGeneral.durationSeconds
            sessionSystemStartTime = existingGeneral.startTime
        }

        return saved
    }

    fun discardTimer() {
        timerJob?.cancel()
        resetTimer()
        _activeSessionId.value = null
        _currentSubject.value = "General"
        viewModelScope.launch {
            val today = DateUtils.getTodayDateString()
            val existingGeneral = repository.getStudySessionByDateAndSubject(today, "General")
            if (existingGeneral != null) {
                _activeSessionId.value = existingGeneral.id
                baseTimeElapsedMs = existingGeneral.durationSeconds * 1000L
                _elapsedSeconds.value = existingGeneral.durationSeconds
                sessionSystemStartTime = existingGeneral.startTime
            }
        }
    }

    private fun resetTimer() {
        _timerState.value = TimerState.IDLE
        _elapsedSeconds.value = 0L
        baseTimeElapsedMs = 0L
        timerStartTimeMs = 0L
        sessionSystemStartTime = 0L
    }

    // 4. Tasks & Todo Management (Google Tasks Style)
    val allTaskLists: StateFlow<List<TaskListEntity>> = repository.allTaskLists
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _selectedListId = MutableStateFlow<Long?>(null)
    val selectedListId: StateFlow<Long?> = _selectedListId.asStateFlow()

    private val _isStarredTabSelected = MutableStateFlow(false)
    val isStarredTabSelected: StateFlow<Boolean> = _isStarredTabSelected.asStateFlow()

    private val _taskSortOrder = MutableStateFlow(TaskSortOrder.MY_ORDER)
    val taskSortOrder: StateFlow<TaskSortOrder> = _taskSortOrder.asStateFlow()

    private val _isCompletedSectionExpanded = MutableStateFlow(true)
    val isCompletedSectionExpanded: StateFlow<Boolean> = _isCompletedSectionExpanded.asStateFlow()

    // Tasks for active selection (List or Starred)
    val currentTasks: StateFlow<List<TaskEntity>> = combine(
        _selectedListId,
        _isStarredTabSelected,
        allTaskLists
    ) { listId, isStarred, lists ->
        if (isStarred) {
            repository.starredTasks
        } else {
            val targetListId = listId ?: lists.firstOrNull { it.isDefault }?.id ?: lists.firstOrNull()?.id ?: 1L
            repository.getTasksForList(targetListId)
        }
    }.flatMapLatest { flow -> flow }
    .combine(_taskSortOrder) { tasks, sortOrder ->
        when (sortOrder) {
            TaskSortOrder.MY_ORDER -> tasks
            TaskSortOrder.STARRED_FIRST -> tasks.sortedWith(compareByDescending<TaskEntity> { it.isStarred }.thenByDescending { it.createdAt })
            TaskSortOrder.ALPHABETICAL -> tasks.sortedBy { it.title.lowercase() }
            TaskSortOrder.NEWEST_FIRST -> tasks.sortedByDescending { it.createdAt }
        }
    }
    .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectList(listId: Long) {
        _isStarredTabSelected.value = false
        _selectedListId.value = listId
    }

    fun selectStarredTab() {
        _isStarredTabSelected.value = true
    }

    fun setSortOrder(order: TaskSortOrder) {
        _taskSortOrder.value = order
    }

    fun toggleCompletedSection() {
        _isCompletedSectionExpanded.value = !_isCompletedSectionExpanded.value
    }

    fun addTask(title: String, notes: String = "", isStarred: Boolean = false) {
        val lists = allTaskLists.value
        val listId = _selectedListId.value ?: lists.firstOrNull { it.isDefault }?.id ?: lists.firstOrNull()?.id ?: 1L
        viewModelScope.launch {
            repository.addTask(
                listId = listId,
                title = title,
                notes = notes,
                isStarred = isStarred || _isStarredTabSelected.value
            )
        }
    }

    fun toggleTaskCompletion(task: TaskEntity) {
        viewModelScope.launch {
            repository.setTaskCompletion(task, !task.isCompleted)
        }
    }

    fun toggleTaskStarred(task: TaskEntity) {
        viewModelScope.launch {
            repository.setTaskStarred(task, !task.isStarred)
        }
    }

    fun updateTask(task: TaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun deleteTask(taskId: Long) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
        }
    }

    fun createNewList(name: String, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val newId = repository.addTaskList(name)
            if (newId > 0) {
                _isStarredTabSelected.value = false
                _selectedListId.value = newId
                onCreated?.invoke(newId)
            }
        }
    }

    fun renameCurrentList(newName: String) {
        val lists = allTaskLists.value
        val listId = _selectedListId.value ?: lists.firstOrNull { it.isDefault }?.id ?: lists.firstOrNull()?.id ?: return
        viewModelScope.launch {
            repository.renameTaskList(listId, newName)
        }
    }

    fun deleteCompletedTasksInCurrentList() {
        val lists = allTaskLists.value
        val listId = _selectedListId.value ?: lists.firstOrNull { it.isDefault }?.id ?: lists.firstOrNull()?.id ?: return
        viewModelScope.launch {
            repository.deleteCompletedTasksForList(listId)
        }
    }

    fun deleteCurrentList() {
        val lists = allTaskLists.value
        val listId = _selectedListId.value ?: lists.firstOrNull { it.isDefault }?.id ?: lists.firstOrNull()?.id ?: return
        val currentList = lists.find { it.id == listId }
        if (currentList?.isDefault == true) return
        viewModelScope.launch {
            repository.deleteTaskList(listId)
            val fallback = lists.firstOrNull { it.isDefault } ?: lists.firstOrNull { it.id != listId }
            _selectedListId.value = fallback?.id
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}

class ProductivityViewModelFactory(
    private val repository: ProductivityRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductivityViewModel::class.java)) {
            return ProductivityViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
