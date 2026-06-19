package com.example.futurefocus.repository

import android.util.Log
import com.example.futurefocus.model.DailyBreakdown
import com.example.futurefocus.model.DailyGoal
import com.example.futurefocus.model.DateRange
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.PeriodStats
import com.example.futurefocus.model.SessionStatus
import com.example.futurefocus.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private companion object {
        const val TAG = "FutureFocusFirestore"
    }

    private val _sessions = MutableStateFlow<List<FocusSession>>(emptyList())
    private val _dailyGoal = MutableStateFlow(DailyGoal(dateKey = todayKey()))
    private var sessionListener: ListenerRegistration? = null
    private var goalListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()
    val dailyGoal: StateFlow<DailyGoal> = _dailyGoal.asStateFlow()

    private val currentUserId: String?
        get() = AuthManager.currentUser?.uid

    init {
        listenAuthState()
    }

    private fun listenAuthState() {
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            if (auth.currentUser != null) {
                observeSessions()
                observeTodayGoal()
            } else {
                sessionListener?.remove()
                sessionListener = null
                goalListener?.remove()
                goalListener = null
                _sessions.value = emptyList()
                _dailyGoal.value = DailyGoal(dateKey = todayKey())
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)

        if (AuthManager.isSignedIn) {
            observeSessions()
            observeTodayGoal()
        }
    }

    fun createSession(durationMinutes: Int, goalId: String? = null, goalTitle: String? = null): FocusSession {
        val session = FocusSession(
            durationMinutes = durationMinutes,
            goalId = goalId,
            goalTitle = goalTitle
        )
        _sessions.update { listOf(session) + it }
        saveSession(session)
        return session
    }

    fun incrementExitAttempt(sessionId: String): FocusSession? {
        var updatedSession: FocusSession? = null
        _sessions.update { sessions ->
            sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(attemptCount = session.attemptCount + 1).also { updatedSession = it }
                } else {
                    session
                }
            }
        }
        updatedSession?.let(::saveSession)
        return updatedSession
    }

    fun completeSession(sessionId: String): FocusSession? {
        return updateStatus(sessionId, SessionStatus.Success)
    }

    fun failSession(sessionId: String): FocusSession? {
        return updateStatus(sessionId, SessionStatus.Failed)
    }

    fun getSession(sessionId: String): FocusSession? = _sessions.value.firstOrNull { it.id == sessionId }

    fun stats(): FocusStats {
        val sessions = _sessions.value
        val successful = sessions.filter { it.status == SessionStatus.Success }
        val failed = sessions.count { it.status == SessionStatus.Failed }
        val todayFocus = successful
            .filter { isToday(it.createdAt) }
            .sumOf { it.durationMinutes }

        return FocusStats(
            totalFocusMinutes = successful.sumOf { it.durationMinutes },
            successfulSessions = successful.size,
            failedSessions = failed,
            focusStreak = calculateStreak(successful),
            todayFocusMinutes = todayFocus,
            dailyGoalMinutes = _dailyGoal.value.targetMinutes
        )
    }

    fun statsForPeriod(dateRange: DateRange): PeriodStats {
        val all = _sessions.value
        val inRange = all.filter { s -> s.createdAt in dateRange.startMillis..dateRange.endMillis }
        val successful = inRange.filter { it.status == SessionStatus.Success }
        val failed = inRange.filter { it.status == SessionStatus.Failed }

        val dailyMap = mutableMapOf<String, MutableList<FocusSession>>()
        inRange.forEach { s ->
            val key = dateKey(s.createdAt)
            dailyMap.getOrPut(key) { mutableListOf() }.add(s)
        }

        val startDate = Calendar.getInstance().apply { timeInMillis = dateRange.startMillis }
        val endDate = Calendar.getInstance().apply { timeInMillis = dateRange.endMillis }
        val breakdown = mutableListOf<DailyBreakdown>()
        val cursor = Calendar.getInstance().apply { timeInMillis = startDate.timeInMillis }
        while (cursor <= endDate || breakdown.size < (dateRange.dayCount)) {
            val key = dateKey(cursor.timeInMillis)
            val daySessions = dailyMap[key].orEmpty()
            val totalMin = daySessions.filter { it.status == SessionStatus.Success }.sumOf { it.durationMinutes }
            breakdown.add(DailyBreakdown(key, totalMin, daySessions.size))
            cursor.add(Calendar.DAY_OF_YEAR, 1)
            if (cursor.timeInMillis > dateRange.endMillis && breakdown.size >= dateRange.dayCount) break
        }

        return PeriodStats(
            totalFocusMinutes = successful.sumOf { it.durationMinutes },
            totalSessions = inRange.size,
            successfulSessions = successful.size,
            failedSessions = failed.size,
            dailyBreakdown = breakdown,
            completedGoalIds = successful.mapNotNull { it.goalId }.distinct()
        )
    }

    fun updateDailyGoal(targetMinutes: Int) {
        val goal = _dailyGoal.value.copy(
            targetMinutes = targetMinutes.coerceAtLeast(1),
            completedMinutes = stats().todayFocusMinutes,
            updatedAt = System.currentTimeMillis()
        )
        _dailyGoal.value = goal
        saveDailyGoal(goal)
    }

    fun refreshTodayGoalProgress() {
        saveDailyGoal(
            _dailyGoal.value.copy(
                completedMinutes = stats().todayFocusMinutes,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    private fun updateStatus(sessionId: String, status: SessionStatus): FocusSession? {
        var updatedSession: FocusSession? = null
        _sessions.update { sessions ->
            sessions.map { session ->
                if (session.id == sessionId) {
                    session.copy(status = status, completedAt = System.currentTimeMillis()).also { updatedSession = it }
                } else {
                    session
                }
            }
        }
        updatedSession?.let(::saveSession)
        if (status == SessionStatus.Success) refreshTodayGoalProgress()
        return updatedSession
    }

    private val userIdForFirestore: String?
        get() = currentUserId

    private fun saveSession(session: FocusSession) {
        val uid = userIdForFirestore ?: return
        val payload = mutableMapOf<String, Any?>(
            "duration" to session.durationMinutes,
            "attempt_count" to session.attemptCount,
            "status" to session.status.name,
            "created_at" to session.createdAt,
            "completed_at" to session.completedAt,
            "goal_id" to session.goalId,
            "goal_title" to session.goalTitle
        )

        firestore.collection("users")
            .document(uid)
            .collection("sessions")
            .document(session.id)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "Session saved: users/$uid/sessions/${session.id}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to save session: ${error.message}", error)
            }
    }

    private fun observeSessions() {
        val uid = userIdForFirestore ?: return
        sessionListener?.remove()
        sessionListener = firestore.collection("users")
            .document(uid)
            .collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe sessions: ${error.message}", error)
                    return@addSnapshotListener
                }

                val loadedSessions = snapshot?.documents
                    ?.mapNotNull { document ->
                        val statusName = document.getString("status") ?: SessionStatus.Running.name
                        FocusSession(
                            id = document.id,
                            durationMinutes = document.getLong("duration")?.toInt() ?: return@mapNotNull null,
                            attemptCount = document.getLong("attempt_count")?.toInt() ?: 0,
                            status = runCatching { SessionStatus.valueOf(statusName) }.getOrDefault(SessionStatus.Running),
                            createdAt = document.getLong("created_at") ?: 0L,
                            completedAt = document.getLong("completed_at"),
                            goalId = document.getString("goal_id"),
                            goalTitle = document.getString("goal_title")
                        )
                    }
                    ?.sortedByDescending { it.createdAt }
                    ?: emptyList()

                _sessions.value = loadedSessions
                refreshTodayGoalProgress()
            }
    }

    private fun observeTodayGoal() {
        val uid = userIdForFirestore ?: return
        goalListener?.remove()
        goalListener = firestore.collection("users")
            .document(uid)
            .collection("daily_goals")
            .document(todayKey())
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe daily goal: ${error.message}", error)
                    return@addSnapshotListener
                }

                val goal = if (snapshot?.exists() == true) {
                    DailyGoal(
                        dateKey = snapshot.id,
                        targetMinutes = snapshot.getLong("target_minutes")?.toInt() ?: 120,
                        completedMinutes = snapshot.getLong("completed_minutes")?.toInt() ?: 0,
                        updatedAt = snapshot.getLong("updated_at") ?: System.currentTimeMillis()
                    )
                } else {
                    DailyGoal(dateKey = todayKey())
                }
                _dailyGoal.value = goal.copy(completedMinutes = stats().todayFocusMinutes)
            }
    }

    private fun saveDailyGoal(goal: DailyGoal) {
        val uid = userIdForFirestore ?: return
        val payload = mapOf(
            "target_minutes" to goal.targetMinutes,
            "completed_minutes" to goal.completedMinutes,
            "updated_at" to goal.updatedAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("daily_goals")
            .document(goal.dateKey)
            .set(payload)
            .addOnSuccessListener {
                Log.d(TAG, "Daily goal saved: users/$uid/daily_goals/${goal.dateKey}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to save daily goal: ${error.message}", error)
            }
    }

    private fun isToday(timestamp: Long): Boolean {
        val target = Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = Calendar.getInstance()
        return target.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
            target.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    }

    private fun calculateStreak(successful: List<FocusSession>): Int {
        val completedDayKeys = successful.map { dateKey(it.createdAt) }.toSet()
        val cursor = Calendar.getInstance()
        var streak = 0

        while (completedDayKeys.contains(dateKey(cursor.timeInMillis))) {
            streak += 1
            cursor.add(Calendar.DAY_OF_YEAR, -1)
        }

        return streak
    }

    private fun todayKey(): String = dateKey(System.currentTimeMillis())

    private fun dateKey(timestamp: Long): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(timestamp))
    }
}
