package com.example.futurefocus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.futurefocus.FutureFocusApplication
import com.example.futurefocus.model.DailyGoal
import com.example.futurefocus.model.DateRange
import com.example.futurefocus.model.ExitMessage
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.Goal
import com.example.futurefocus.model.PeriodStats
import com.example.futurefocus.model.Subtask
import com.example.futurefocus.repository.FocusRepository
import com.example.futurefocus.repository.GoalRepository
import com.example.futurefocus.repository.MessageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FocusViewModel(
    private val focusRepository: FocusRepository = FocusRepository(),
    private val goalRepository: GoalRepository = GoalRepository(),
    private val messageRepository: MessageRepository = MessageRepository(
        database = FutureFocusApplication.instance.database
    )
) : ViewModel() {
    val sessions: StateFlow<List<FocusSession>> = focusRepository.sessions
    val dailyGoal: StateFlow<DailyGoal> = focusRepository.dailyGoal
    val goals: StateFlow<List<Goal>> = goalRepository.goals
    val subtasks: StateFlow<Map<String, List<Subtask>>> = goalRepository.subtasks

    private val _showSessionFailed = MutableStateFlow(false)
    val showSessionFailed: StateFlow<Boolean> = _showSessionFailed

    private val _completedGoalId = MutableStateFlow<String?>(null)
    val completedGoalId: StateFlow<String?> = _completedGoalId

    fun markSessionFailed() {
        _showSessionFailed.value = true
    }

    fun dismissSessionFailed() {
        _showSessionFailed.value = false
    }

    fun dismissCompletedGoal() {
        _completedGoalId.value = null
    }

    init {
        preloadQuotes()
    }

    fun startSession(durationMinutes: Int, goalId: String? = null, goalTitle: String? = null): FocusSession {
        return focusRepository.createSession(durationMinutes.coerceAtLeast(1), goalId, goalTitle)
    }

    fun registerExitAttempt(sessionId: String): ExitMessage {
        val session = focusRepository.incrementExitAttempt(sessionId)
        return messageRepository.getExitMessage(session?.attemptCount ?: 1)
    }

    fun preloadQuotes() {
        viewModelScope.launch {
            messageRepository.preloadQuotes()
        }
    }

    fun completeSession(sessionId: String): FocusSession? {
        val session = focusRepository.completeSession(sessionId)
        session?.let {
            if (it.goalId != null) {
                goalRepository.deductRemainingHours(it.goalId, it.durationMinutes / 60f)
                goalRepository.checkAndCompleteGoal(it.goalId)
                val goal = goals.value.firstOrNull { g -> g.id == it.goalId }
                if (goal != null) {
                    val newRemaining = goal.remainingHours - (it.durationMinutes / 60f)
                    if (newRemaining <= 0f) {
                        _completedGoalId.value = it.goalId
                    }
                }
            }
        }
        return session
    }

    fun failSession(sessionId: String): FocusSession? = focusRepository.failSession(sessionId)

    fun getSession(sessionId: String): FocusSession? = focusRepository.getSession(sessionId)

    fun getGoal(id: String): Goal? = goals.value.firstOrNull { it.id == id }

    fun stats(): FocusStats = focusRepository.stats()

    fun statsForPeriod(dateRange: DateRange): PeriodStats = focusRepository.statsForPeriod(dateRange)

    fun updateDailyGoal(targetMinutes: Int) {
        viewModelScope.launch {
            focusRepository.updateDailyGoal(targetMinutes)
        }
    }

    fun createGoal(goal: Goal, subtasks: List<Subtask>) {
        goalRepository.createGoal(goal)
        subtasks.forEach { goalRepository.addSubtask(goal.id, it) }
    }

    fun toggleSubtask(goalId: String, subtaskId: String, isCompleted: Boolean) {
        goalRepository.toggleSubtask(goalId, subtaskId, isCompleted)
        goalRepository.checkAndCompleteGoal(goalId)
    }

    fun deleteSubtask(goalId: String, subtaskId: String) {
        goalRepository.deleteSubtask(goalId, subtaskId)
    }
}
