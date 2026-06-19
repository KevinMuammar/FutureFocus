package com.example.futurefocus.service

import android.util.Log

object FocusSessionTracker {
    var isSessionActive = false
    var activeSessionId: String? = null
    var activeDurationMinutes: Int = 0
    var onForceExit: (() -> Unit)? = null
    var onAppLeftForeground: (() -> Unit)? = null
    var onNavigationAttempt: (() -> Unit)? = null
    @Volatile
    var isAppInForeground = false
        private set
    @Volatile
    private var suppressBackUntilMs = 0L

    fun startSession(sessionId: String, durationMinutes: Int) {
        isSessionActive = true
        activeSessionId = sessionId
        activeDurationMinutes = durationMinutes
    }

    fun endSession() {
        isSessionActive = false
        activeSessionId = null
        activeDurationMinutes = 0
        onForceExit = null
        onNavigationAttempt = null
        suppressBackUntilMs = 0L
    }

    fun forceExit() {
        Log.d("FocusSessionTracker", "forceExit called for session: $activeSessionId")
        onForceExit?.invoke()
        endSession()
    }

    fun markAppForegrounded() {
        isAppInForeground = true
    }

    fun markAppBackgrounded() {
        isAppInForeground = false
        if (isSessionActive) {
            onAppLeftForeground?.invoke()
        }
    }

    fun notifyNavigationAttempt() {
        if (isSessionActive) {
            onNavigationAttempt?.invoke()
        }
    }

    fun suppressNextRedirectBack() {
        suppressBackUntilMs = System.currentTimeMillis() + REDIRECT_BACK_SUPPRESSION_MS
    }

    fun consumeRedirectBackIfPending(): Boolean {
        val now = System.currentTimeMillis()
        if (now > suppressBackUntilMs) {
            suppressBackUntilMs = 0L
            return false
        }

        suppressBackUntilMs = 0L
        return true
    }

    private const val REDIRECT_BACK_SUPPRESSION_MS = 1_500L
}
