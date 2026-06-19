package com.example.futurefocus.repository

import android.util.Log
import com.example.futurefocus.model.Goal
import com.example.futurefocus.model.Subtask
import com.example.futurefocus.utils.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GoalRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private companion object {
        const val TAG = "FutureFocusGoal"
    }

    private val _goals = MutableStateFlow<List<Goal>>(emptyList())
    private val _subtasks = MutableStateFlow<Map<String, List<Subtask>>>(emptyMap())
    private var goalsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    val goals: StateFlow<List<Goal>> = _goals.asStateFlow()
    val subtasks: StateFlow<Map<String, List<Subtask>>> = _subtasks.asStateFlow()

    private val currentUserId: String?
        get() = AuthManager.currentUser?.uid

    init {
        listenAuthState()
    }

    private fun listenAuthState() {
        authStateListener?.let { FirebaseAuth.getInstance().removeAuthStateListener(it) }
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
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
            observeGoals()
        }
    }

    private fun uidOrSkip(): String? = currentUserId

    fun createGoal(goal: Goal) {
        val uid = uidOrSkip() ?: return
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
            .addOnSuccessListener { Log.d(TAG, "Goal created: ${goal.id}") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to create goal", e) }
    }

    fun addSubtask(goalId: String, subtask: Subtask) {
        val uid = uidOrSkip() ?: return
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
            .addOnSuccessListener { Log.d(TAG, "Subtask added: ${subtask.id}") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to add subtask", e) }
    }

    fun toggleSubtask(goalId: String, subtaskId: String, isCompleted: Boolean) {
        val uid = uidOrSkip() ?: return
        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .document(subtaskId)
            .update("isCompleted", isCompleted)
            .addOnSuccessListener { Log.d(TAG, "Subtask toggled: $subtaskId -> $isCompleted") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to toggle subtask", e) }
    }

    fun deleteSubtask(goalId: String, subtaskId: String) {
        val uid = uidOrSkip() ?: return
        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .collection("subtasks")
            .document(subtaskId)
            .delete()
            .addOnSuccessListener { Log.d(TAG, "Subtask deleted: $subtaskId") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to delete subtask", e) }
    }

    fun deductRemainingHours(goalId: String, hours: Float) {
        val uid = uidOrSkip() ?: return
        val goal = _goals.value.firstOrNull { it.id == goalId } ?: return
        val newRemaining = (goal.remainingHours - hours).coerceAtLeast(0f)
        val isComplete = newRemaining <= 0f

        val updates = mutableMapOf<String, Any>("remainingHours" to newRemaining)
        if (isComplete) updates["isCompleted"] = true

        firestore.collection("users")
            .document(uid)
            .collection("goals")
            .document(goalId)
            .update(updates)
            .addOnSuccessListener { Log.d(TAG, "Goal hours updated: $goalId -> $newRemaining") }
            .addOnFailureListener { e -> Log.e(TAG, "Failed to update goal hours", e) }
    }

    fun checkAndCompleteGoal(goalId: String) {
        val uid = uidOrSkip() ?: return
        val goal = _goals.value.firstOrNull { it.id == goalId } ?: return
        val subs = _subtasks.value[goalId].orEmpty()
        val allSubtasksDone = subs.isNotEmpty() && subs.all { it.isCompleted }
        val timeComplete = goal.remainingHours <= 0f

        if (allSubtasksDone || timeComplete) {
            firestore.collection("users")
                .document(uid)
                .collection("goals")
                .document(goalId)
                .update("isCompleted", true)
                .addOnSuccessListener { Log.d(TAG, "Goal completed: $goalId") }
                .addOnFailureListener { e -> Log.e(TAG, "Failed to complete goal", e) }
        }
    }

    private fun observeGoals() {
        val uid = uidOrSkip() ?: return
        goalsListener?.remove()
        goalsListener = firestore.collection("users")
            .document(uid)
            .collection("goals")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Failed to observe goals", error)
                    return@addSnapshotListener
                }

                val loaded = snapshot?.documents?.mapNotNull { doc ->
                    Goal(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        totalHours = doc.getDouble("totalHours")?.toFloat() ?: return@mapNotNull null,
                        remainingHours = doc.getDouble("remainingHours")?.toFloat() ?: doc.getDouble("totalHours")?.toFloat() ?: 0f,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()

                _goals.value = loaded.sortedByDescending { it.createdAt }
                loaded.forEach { observeSubtasks(it.id) }
            }
    }

    private fun observeSubtasks(goalId: String) {
        val uid = uidOrSkip() ?: return
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
                    Subtask(
                        id = doc.id,
                        title = doc.getString("title") ?: return@mapNotNull null,
                        isCompleted = doc.getBoolean("isCompleted") ?: false,
                        createdAt = doc.getLong("createdAt") ?: 0L
                    )
                }.orEmpty()

                _subtasks.value = _subtasks.value + (goalId to loaded)
            }
    }
}
