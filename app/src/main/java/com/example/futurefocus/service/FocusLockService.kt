package com.example.futurefocus.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class FocusLockService : AccessibilityService() {

    private var overlayManager: FocusOverlayManager? = null
    private val handler = Handler(Looper.getMainLooper())
    private var currentSessionId: String? = null
    private var currentSessionSwitches = 0
    private var lastPackage = ""
    private var lastActionTime = 0L
    private var lastNavigationMessageTime = 0L
    private var homePackage: String? = null
    private val returnToAppRunnable = object : Runnable {
        override fun run() {
            if (!FocusSessionTracker.isSessionActive || FocusSessionTracker.isAppInForeground) {
                overlayManager?.hide()
                return
            }

            returnFromSystemNavigation()
            handler.postDelayed(this, RETURN_RETRY_MS)
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "AccessibilityService connected")
        overlayManager = FocusOverlayManager()
        FocusSessionTracker.onAppLeftForeground = {
            handler.removeCallbacks(recoverAfterActivityPauseRunnable)
            handler.postDelayed(recoverAfterActivityPauseRunnable, PAUSE_RECOVERY_DELAY_MS)
        }
        homePackage = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .let { packageManager.resolveActivity(it, 0)?.activityInfo?.packageName }
    }

    private val recoverAfterActivityPauseRunnable = Runnable {
        if (FocusSessionTracker.isSessionActive && !FocusSessionTracker.isAppInForeground) {
            recoverFromSystemNavigation()
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (!FocusSessionTracker.isSessionActive) {
            handler.removeCallbacks(returnToAppRunnable)
            overlayManager?.hide()
            return
        }

        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return
        val now = System.currentTimeMillis()

        // Sinkronisasi session ID untuk reset counter jika sesi berganti
        if (FocusSessionTracker.activeSessionId != currentSessionId) {
            currentSessionId = FocusSessionTracker.activeSessionId
            currentSessionSwitches = 0
            lastPackage = ""
            lastActionTime = 0L
            lastNavigationMessageTime = 0L
        }

        val isOurApp = pkg == packageName

        if (isOurApp) {
            handler.removeCallbacks(recoverAfterActivityPauseRunnable)
            handler.removeCallbacks(returnToAppRunnable)
            overlayManager?.hide()
            lastPackage = pkg
            return
        }

        val isSystemNavigation = isHomeOrRecents(pkg)

        // Overview/Home dapat mengirim paket System UI yang sama saat ditekan cepat.
        // Jangan jatuhkan event ini dengan cooldown; recovery harus berjalan setiap kali.
        if (isSystemNavigation) {
            lastPackage = pkg
            lastActionTime = now
            Log.d(TAG, "Recovering from system navigation: $pkg")
            recoverFromSystemNavigation()
            return
        }

        // Hindari spamming event berulang dari aplikasi luar yang sama.
        if (pkg == lastPackage && now < lastActionTime + COOLDOWN_MS) return

        // Home dan recent hanya dialihkan kembali; keduanya tidak dihitung sebagai keluar sesi.
        if (lastPackage == packageName || lastPackage.isEmpty()) {
            currentSessionSwitches++
            Log.d(TAG, "Violation detected! Total switches in this session: $currentSessionSwitches")

            if (currentSessionSwitches >= MAX_SWITCHES) {
                Log.d(TAG, "Max switches reached. Forcing exit.")
                FocusSessionTracker.forceExit()
                overlayManager?.hide()
                return
            }
        }

        lastPackage = pkg
        lastActionTime = now

        Log.d(TAG, "Blocking access to: $pkg (systemNavigation=$isSystemNavigation)")

        overlayManager?.show()

        // Aplikasi luar juga merupakan usaha meninggalkan sesi focus.
        FocusSessionTracker.notifyNavigationAttempt()
        FocusSessionTracker.suppressNextRedirectBack()
        performGlobalAction(GLOBAL_ACTION_BACK)
        handler.removeCallbacks(returnToAppRunnable)
        handler.postDelayed(returnToAppRunnable, BRING_TO_FRONT_DELAY_MS)
    }

    private fun isHomeOrRecents(pkg: String): Boolean {
        return pkg == homePackage || pkg.contains(SYSTEM_UI_PACKAGE_FRAGMENT, ignoreCase = true)
    }

    private fun recoverFromSystemNavigation() {
        handler.removeCallbacks(recoverAfterActivityPauseRunnable)
        handler.removeCallbacks(returnToAppRunnable)
        returnFromSystemNavigation()
    }

    private fun returnFromSystemNavigation(notifyAttempt: Boolean = false) {
        overlayManager?.hide()
        handler.removeCallbacks(recoverAfterActivityPauseRunnable)
        handler.removeCallbacks(returnToAppRunnable)
        FocusSessionTracker.suppressNextRedirectBack()
        bringAppToFront()
    }

    override fun onInterrupt() {
        Log.d(TAG, "AccessibilityService interrupted")
    }

    override fun onDestroy() {
        super.onDestroy()
        FocusSessionTracker.onAppLeftForeground = null
        handler.removeCallbacks(recoverAfterActivityPauseRunnable)
        handler.removeCallbacks(returnToAppRunnable)
        overlayManager?.hide()
        Log.d(TAG, "AccessibilityService destroyed")
    }

    private fun bringAppToFront() {
        val intent = packageManager.getLaunchIntentForPackage(packageName) ?: return
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP
        try {
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to bring app to front: ${e.message}")
        }
    }

    companion object {
        private const val TAG = "FocusLockService"
        private const val MAX_SWITCHES = 10
        private const val BRING_TO_FRONT_DELAY_MS = 120L
        private const val PAUSE_RECOVERY_DELAY_MS = 90L
        private const val RETURN_RETRY_MS = 450L
        private const val NAVIGATION_MESSAGE_DEBOUNCE_MS = 250L
        private const val COOLDOWN_MS = 800L
        private const val SYSTEM_UI_PACKAGE_FRAGMENT = "systemui"
    }
}
