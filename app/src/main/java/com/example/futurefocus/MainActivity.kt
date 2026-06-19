package com.example.futurefocus

import android.Manifest
import android.app.ActivityManager
import android.os.Bundle
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import com.example.futurefocus.navigation.FutureFocusApp
import com.example.futurefocus.service.FocusSessionTracker
import com.example.futurefocus.ui.theme.FutureFocusTheme

class MainActivity : ComponentActivity() {
    fun setExcludedFromRecentsForFocus(excluded: Boolean) {
        val activityManager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
        activityManager.appTasks
            .firstOrNull { it.taskInfo.taskId == taskId }
            ?.setExcludeFromRecents(excluded)
    }

    override fun onResume() {
        super.onResume()
        FocusSessionTracker.markAppForegrounded()
    }

    override fun onPause() {
        FocusSessionTracker.markAppBackgrounded()
        super.onPause()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_FutureFocus)
        super.onCreate(savedInstanceState)
        setExcludedFromRecentsForFocus(FocusSessionTracker.isSessionActive)
        enableEdgeToEdge()
        setContent {
            FutureFocusTheme {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { }
                    )

                    LaunchedEffect(Unit) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                FutureFocusApp()
            }
        }
    }
}
