package com.example.futurefocus.repository

import android.util.Log
import com.example.futurefocus.data.local.AppDatabase
import com.example.futurefocus.data.local.DailyGoalEntity
import com.example.futurefocus.data.local.SessionEntity
import com.example.futurefocus.data.local.toDailyGoalEntity
import com.example.futurefocus.data.local.toSessionEntity
import com.example.futurefocus.data.local.SessionAchievementEntity
import com.example.futurefocus.model.Achievement
import com.example.futurefocus.model.DailyBreakdown
import com.example.futurefocus.model.NewAchievement
import com.example.futurefocus.model.DailyGoal
import com.example.futurefocus.model.DateRange
import com.example.futurefocus.model.FocusSession
import com.example.futurefocus.model.FocusStats
import com.example.futurefocus.model.PeriodStats
import com.example.futurefocus.model.SessionStatus
import com.example.futurefocus.utils.AuthManager
import com.example.futurefocus.utils.NetworkMonitor
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class FocusRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor
) {
    private companion object {
        const val TAG = "FutureFocusFirestore"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _sessions = MutableStateFlow<List<FocusSession>>(emptyList())
    private val _dailyGoal = MutableStateFlow(DailyGoal(dateKey = todayKey()))
    private val _sessionAchievements = MutableStateFlow<Map<String, List<Achievement>>>(emptyMap())
    private val _currentUserId = MutableStateFlow("local")
    private var sessionListener: ListenerRegistration? = null
    private var goalListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    val sessions: StateFlow<List<FocusSession>> = _sessions.asStateFlow()
    val dailyGoal: StateFlow<DailyGoal> = _dailyGoal.asStateFlow()
    val sessionAchievements: StateFlow<Map<String, List<Achievement>>> = _sessionAchievements.asStateFlow()

    private val firestoreUid: String?
        get() = AuthManager.currentUser?.uid

    init {
        listenAuthState()
        observeLocalSessions()
        observeLocalDailyGoal()
        observeSessionAchievements()

        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) syncPendingItems()
            }
        }
    }

    private fun observeLocalSessions() {
        scope.launch {
            _currentUserId.collectLatest { userId ->
                database.sessionDao().observeAll(userId).collect { entities ->
                    _sessions.value = entities.map { it.toDomainModel() }
                }
            }
        }
    }

    private fun observeSessionAchievements() {
        scope.launch {
            database.sessionAchievementDao().observeAll().collect { entities ->
                _sessionAchievements.value = entities.groupBy(
                    keySelector = { it.sessionId },
                    valueTransform = { it.toDomainModel() }
                )
            }
        }
    }

    private fun observeLocalDailyGoal() {
        scope.launch {
            _currentUserId.collectLatest { userId ->
                database.dailyGoalDao().observeByDate(todayKey(), userId).collect { entity ->
                    if (entity != null) {
                        _dailyGoal.value = entity.toDomainModel().copy(
                            completedMinutes = stats().todayFocusMinutes
                        )
                    }
                }
            }
        }
    }

    private fun listenAuthState() {
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            _currentUserId.value = auth.currentUser?.uid ?: "local"
            if (auth.currentUser != null) {
                observeSessions()
                observeTodayGoal()
                scope.launch { syncPendingItems() }
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
            _currentUserId.value = AuthManager.currentUser?.uid ?: "local"
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
        scope.launch {
            database.sessionDao().insert(session.toSessionEntity(_currentUserId.value, "PENDING"))
            syncSessionToFirestore(session)
        }
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
        updatedSession?.let { session ->
            scope.launch {
                database.sessionDao().updateAttemptCount(session.id, session.attemptCount)
                syncSessionToFirestore(session)
            }
        }
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
        scope.launch {
            database.dailyGoalDao().insert(goal.toDailyGoalEntity(_currentUserId.value, "PENDING"))
            syncDailyGoalToFirestore(goal)
        }
    }

    fun saveSessionAchievements(sessionId: String, achievements: List<NewAchievement>) {
        scope.launch {
            val entities = achievements.map {
                SessionAchievementEntity.fromAchievement(sessionId, it.achievement)
            }
            database.sessionAchievementDao().insertAll(entities)
        }
    }

    fun refreshTodayGoalProgress() {
        val goal = _dailyGoal.value.copy(
            completedMinutes = stats().todayFocusMinutes,
            updatedAt = System.currentTimeMillis()
        )
        _dailyGoal.value = goal
        scope.launch {
            database.dailyGoalDao().insert(goal.toDailyGoalEntity(_currentUserId.value, "PENDING"))
            syncDailyGoalToFirestore(goal)
        }
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
        updatedSession?.let { session ->
            scope.launch {
                database.sessionDao().updateStatus(session.id, status.name, session.completedAt ?: System.currentTimeMillis())
                syncSessionToFirestore(session)
            }
        }
        if (status == SessionStatus.Success) refreshTodayGoalProgress()
        return updatedSession
    }

    private fun syncSessionToFirestore(session: FocusSession) {
        val uid = firestoreUid ?: return
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
                scope.launch { database.sessionDao().markSynced(session.id) }
                Log.d(TAG, "Session synced: ${session.id}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to sync session: ${error.message}", error)
            }
    }

    private fun syncDailyGoalToFirestore(goal: DailyGoal) {
        val uid = firestoreUid ?: return

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
                scope.launch { database.dailyGoalDao().markSynced(goal.dateKey, uid) }
                Log.d(TAG, "Daily goal synced: ${goal.dateKey}")
            }
            .addOnFailureListener { error ->
                Log.e(TAG, "Failed to sync daily goal: ${error.message}", error)
            }
    }

    private suspend fun syncPendingItems() {
        val uid = firestoreUid ?: return

        val pendingSessions = database.sessionDao().getPendingItems(_currentUserId.value)
        for (entity in pendingSessions) {
            val payload = mutableMapOf<String, Any?>(
                "duration" to entity.durationMinutes,
                "attempt_count" to entity.attemptCount,
                "status" to entity.status,
                "created_at" to entity.createdAt,
                "completed_at" to entity.completedAt,
                "goal_id" to entity.goalId,
                "goal_title" to entity.goalTitle
            )
            firestore.collection("users")
                .document(uid)
                .collection("sessions")
                .document(entity.id)
                .set(payload)
                .addOnSuccessListener {
                    scope.launch { database.sessionDao().markSynced(entity.id) }
                    Log.d(TAG, "Pending session synced: ${entity.id}")
                }
        }

        val pendingGoals = database.dailyGoalDao().getPendingItems(_currentUserId.value)
        for (entity in pendingGoals) {
            val payload = mapOf(
                "target_minutes" to entity.targetMinutes,
                "completed_minutes" to entity.completedMinutes,
                "updated_at" to entity.updatedAt
            )
            firestore.collection("users")
                .document(uid)
                .collection("daily_goals")
                .document(entity.dateKey)
                .set(payload)
                .addOnSuccessListener {
                    scope.launch { database.dailyGoalDao().markSynced(entity.dateKey, uid) }
                    Log.d(TAG, "Pending daily goal synced: ${entity.dateKey}")
                }
        }
    }

    private fun observeSessions() {
        val uid = firestoreUid ?: return
        sessionListener?.remove()
        sessionListener = firestore.collection("users")
            .document(uid)
            .collection("sessions")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe sessions: ${error.message}", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            scope.launch { database.sessionDao().deleteById(change.document.id) }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val doc = change.document
                            val statusName = doc.getString("status") ?: SessionStatus.Running.name
                            val session = SessionEntity(
                                id = doc.id,
                                userId = uid,
                                durationMinutes = doc.getLong("duration")?.toInt() ?: return@forEach,
                                attemptCount = doc.getLong("attempt_count")?.toInt() ?: 0,
                                status = statusName,
                                createdAt = doc.getLong("created_at") ?: 0L,
                                completedAt = doc.getLong("completed_at"),
                                goalId = doc.getString("goal_id"),
                                goalTitle = doc.getString("goal_title"),
                                syncStatus = "SYNCED"
                            )
                            scope.launch {
                                database.sessionDao().insert(session)
                                refreshTodayGoalProgress()
                            }
                        }
                    }
                }
            }
    }

    private fun observeTodayGoal() {
        val uid = firestoreUid ?: return
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

                if (snapshot?.exists() == true) {
                    val entity = DailyGoalEntity(
                        dateKey = snapshot.id,
                        userId = uid,
                        targetMinutes = snapshot.getLong("target_minutes")?.toInt() ?: 120,
                        completedMinutes = snapshot.getLong("completed_minutes")?.toInt() ?: 0,
                        updatedAt = snapshot.getLong("updated_at") ?: System.currentTimeMillis(),
                        syncStatus = "SYNCED"
                    )
                    scope.launch {
                        database.dailyGoalDao().insert(entity)
                    }
                } else if (snapshot != null) {
                    scope.launch {
                        database.dailyGoalDao().deleteByDateKey(snapshot.id, uid)
                    }
                }
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
