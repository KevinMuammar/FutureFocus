package com.example.futurefocus.repository

import android.util.Log
import com.example.futurefocus.data.local.AppDatabase
import com.example.futurefocus.data.local.GoalEntity
import com.example.futurefocus.data.local.SubtaskEntity
import com.example.futurefocus.data.local.toGoalEntity
import com.example.futurefocus.data.local.toSubtaskEntity
import com.example.futurefocus.model.Goal
import com.example.futurefocus.model.Subtask
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
import kotlinx.coroutines.launch

class GoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val database: AppDatabase,
    private val networkMonitor: NetworkMonitor
) {
    private companion object {
        const val TAG = "FutureFocusGoal"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    private val _subtasks = MutableStateFlow<Map<String, List<Subtask>>>(emptyMap())
    private val _currentUserId = MutableStateFlow("local")
    private var goalsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()
    val subtasks: StateFlow<Map<String, List<Subtask>>> = _subtasks.asStateFlow()

    private val firestoreUid: String?
        get() = AuthManager.currentUser?.uid

    init {
        listenAuthState()
        observeLocalGoals()

        scope.launch {
            networkMonitor.isOnline.collect { online ->
                if (online) syncPendingItems()
            }
        }
    }

    private fun observeLocalGoals() {
        scope.launch {
            _currentUserId.collectLatest { userId ->
                database.goalDao().observeAll(userId).collect { entities ->
                    _goals.value = entities.map { it.toDomainModel() }
                    entities.forEach { goal ->
                        scope.launch {
                            database.subtaskDao().observeByGoalId(goal.id, userId).collect { subtaskEntities ->
                                val existing = _subtasks.value.toMutableMap()
                                existing[goal.id] = subtaskEntities.map { it.toDomainModel() }
                                _subtasks.value = existing
                            }
                        }
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
                observeGoals()
            } else {
                goalsListener?.remove()
                goalsListener = null
                _goals.value = emptyList()
                _subtasks.value = emptyMap()
            }
        }
        FirebaseAuth.getInstance().addAuthStateListener(authStateListener!!)

        if (AuthManager.isSignedIn) {
            _currentUserId.value = AuthManager.currentUser?.uid ?: "local"
            observeGoals()
        }
    }

    fun createGoal(goal: Goal) {
        scope.launch {
            database.goalDao().insert(goal.toGoalEntity(_currentUserId.value, "PENDING"))
            syncGoalToFirestore(goal)
        }
    }

    fun updateGoal(goal: Goal) {
        scope.launch {
            database.goalDao().updateGoal(
                id = goal.id,
                title = goal.title,
                description = goal.description,
                totalHours = goal.totalHours,
                remainingHours = goal.remainingHours
            )
            syncGoalToFirestore(goal)
        }
    }

    fun deleteGoal(goalId: String) {
        scope.launch {
            database.goalDao().deleteById(goalId)
            database.subtaskDao().deleteByGoalId(goalId, _currentUserId.value)
            syncGoalDeleteToFirestore(goalId)
        }
    }

    fun addSubtask(goalId: String, subtask: Subtask) {
        scope.launch {
            database.subtaskDao().insert(subtask.toSubtaskEntity(_currentUserId.value, goalId, "PENDING"))
            syncSubtaskToFirestore(goalId, subtask)
        }
    }

    fun toggleSubtask(goalId: String, subtaskId: String, isCompleted: Boolean) {
        scope.launch {
            database.subtaskDao().updateCompleted(subtaskId, isCompleted)
            syncSubtaskToggleToFirestore(goalId, subtaskId, isCompleted)
        }
    }

    fun deleteSubtask(goalId: String, subtaskId: String) {
        scope.launch {
            database.subtaskDao().delete(subtaskId)
            syncSubtaskDeleteToFirestore(goalId, subtaskId)
        }
    }

    fun deductRemainingHours(goalId: String, hours: Float) {
        val goal = _goals.value.firstOrNull { it.id == goalId } ?: return
        val newRemaining = (goal.remainingHours - hours).coerceAtLeast(0f)
        val isComplete = newRemaining <= 0f

        scope.launch {
            database.goalDao().updateHours(goalId, newRemaining, isComplete)
            syncGoalHoursToFirestore(goalId, newRemaining, isComplete)
        }
    }

    fun checkAndCompleteGoal(goalId: String) {
        val goal = _goals.value.firstOrNull { it.id == goalId } ?: return
        val subs = _subtasks.value[goalId].orEmpty()
        val allSubtasksDone = subs.isNotEmpty() && subs.all { it.isCompleted }
        val timeComplete = goal.remainingHours <= 0f

        if (allSubtasksDone || timeComplete) {
            scope.launch {
                database.goalDao().updateCompleted(goalId, true)
                syncGoalCompleteToFirestore(goalId)
            }
        }
    }

    private fun syncGoalToFirestore(goal: Goal) {
        val uid = firestoreUid ?: return

        val payload = mapOf(
            "title" to goal.title,
            "description" to goal.description,
            "totalHours" to goal.totalHours,
            "remainingHours" to goal.remainingHours,
            "isCompleted" to goal.isCompleted,
            "createdAt" to goal.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goal.id)
            .set(payload)
            .addOnSuccessListener {
                scope.launch { database.goalDao().markSynced(goal.id) }
                Log.d(TAG, "Goal synced: ${goal.id}")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync goal", e) }
    }

    private fun syncSubtaskToFirestore(goalId: String, subtask: Subtask) {
        val uid = firestoreUid ?: return

        val payload = mapOf(
            "title" to subtask.title,
            "isCompleted" to subtask.isCompleted,
            "createdAt" to subtask.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .document(subtask.id)
            .set(payload)
            .addOnSuccessListener {
                scope.launch { database.subtaskDao().markSynced(subtask.id) }
                Log.d(TAG, "Subtask synced: ${subtask.id}")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync subtask", e) }
    }

    private fun syncSubtaskToggleToFirestore(goalId: String, subtaskId: String, isCompleted: Boolean) {
        val uid = firestoreUid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .document(subtaskId)
            .set(mapOf("isCompleted" to isCompleted))
            .addOnSuccessListener {
                scope.launch { database.subtaskDao().markSynced(subtaskId) }
                Log.d(TAG, "Subtask toggled synced: $subtaskId -> $isCompleted")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync subtask toggle", e) }
    }

    private fun syncSubtaskDeleteToFirestore(goalId: String, subtaskId: String) {
        val uid = firestoreUid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .document(subtaskId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Subtask delete synced: $subtaskId") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync subtask delete", e) }
    }

    private fun syncGoalHoursToFirestore(goalId: String, remainingHours: Float, isComplete: Boolean) {
        val uid = firestoreUid ?: return

        val payload = mutableMapOf<String, Any>(
            "remainingHours" to remainingHours,
            "totalHours" to (_goals.value.firstOrNull { it.id == goalId }?.totalHours ?: 0f),
            "isCompleted" to isComplete
        )

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .set(payload)
            .addOnSuccessListener {
                scope.launch { database.goalDao().markSynced(goalId) }
                Log.d(TAG, "Goal hours synced: $goalId -> $remainingHours")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync goal hours", e) }
    }

    private fun syncGoalCompleteToFirestore(goalId: String) {
        val uid = firestoreUid ?: return

        val goal = _goals.value.firstOrNull { it.id == goalId }
        if (goal == null) return

        val payload = mapOf(
            "title" to goal.title,
            "description" to goal.description,
            "totalHours" to goal.totalHours,
            "remainingHours" to goal.remainingHours,
            "isCompleted" to true,
            "createdAt" to goal.createdAt
        )

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .set(payload)
            .addOnSuccessListener {
                scope.launch { database.goalDao().markSynced(goalId) }
                Log.d(TAG, "Goal complete synced: $goalId")
            }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to sync goal complete", e) }
    }

    private fun syncGoalDeleteToFirestore(goalId: String) {
        val uid = firestoreUid ?: return

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Goal deleted from Firestore: $goalId") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to delete goal from Firestore", e) }
    }

    private suspend fun syncPendingItems() {
        val uid = firestoreUid ?: return

        val pendingGoals = database.goalDao().getPendingItems(_currentUserId.value)
        for (entity in pendingGoals) {
            val payload = mapOf(
                "title" to entity.title,
                "description" to entity.description,
                "totalHours" to entity.totalHours,
                "remainingHours" to entity.remainingHours,
                "isCompleted" to entity.isCompleted,
                "createdAt" to entity.createdAt
            )
            firestore.collection("users")
                .document(uid)
                .collection("goals")
                .document(entity.id)
                .set(payload)
                .addOnSuccessListener {
                    scope.launch { database.goalDao().markSynced(entity.id) }
                    Log.d(TAG, "Pending goal synced: ${entity.id}")
                }
        }

        val pendingSubtasks = database.subtaskDao().getPendingItems(_currentUserId.value)
        for (entity in pendingSubtasks) {
            val payload = mapOf(
                "title" to entity.title,
                "isCompleted" to entity.isCompleted,
                "createdAt" to entity.createdAt
            )
            firestore.collection("users")
                .document(uid)
                .collection("goals")
                .document(entity.goalId)
                .collection("subtasks")
                .document(entity.id)
                .set(payload)
                .addOnSuccessListener {
                    scope.launch { database.subtaskDao().markSynced(entity.id) }
                    Log.d(TAG, "Pending subtask synced: ${entity.id}")
                }
        }
    }

    private fun observeGoals() {
        val uid = firestoreUid ?: return
        goalsListener?.remove()
        goalsListener = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe goals", error)
                    return@addSnapshotListener
                }

                snapshot?.documentChanges?.forEach { change ->
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            scope.launch {
                                database.goalDao().deleteById(change.document.id)
                                database.subtaskDao().deleteByGoalId(change.document.id, uid)
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.ADDED,
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val doc = change.document
                            val goal = GoalEntity(
                                id = doc.id,
                                userId = uid,
                                title = doc.getString("title") ?: return@forEach,
                                description = doc.getString("description") ?: "",
                                totalHours = doc.getDouble("totalHours")?.toFloat() ?: return@forEach,
                                remainingHours = doc.getDouble("remainingHours")?.toFloat() ?: doc.getDouble("totalHours")?.toFloat() ?: 0f,
                                isCompleted = doc.getBoolean("isCompleted") ?: false,
                                createdAt = doc.getLong("createdAt") ?: 0L,
                                syncStatus = "SYNCED"
                            )
                            scope.launch {
                                database.goalDao().insert(goal)
                                observeSubtasks(doc.id)
                            }
                        }
                    }
                }
            }
    }

    private fun observeSubtasks(goalId: String) {
        val uid = firestoreUid ?: return
        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe subtasks for $goalId", error)
                    return@addSnapshotListener
                }

                val loaded = snapshot?.documents?.mapNotNull { doc ->
                    SubtaskEntity(
                        id = doc.id,
                        userId = uid,
                        goalId = goalId,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        syncStatus = "SYNCED"
                    )
                }.orEmpty()

                scope.launch {
                    database.subtaskDao().deleteByGoalId(goalId, uid)
                    database.subtaskDao().insertAll(loaded)
                }
            }
    }
}
