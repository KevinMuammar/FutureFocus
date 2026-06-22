package com.example.futurefocus.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.futurefocus.R

class FocusForegroundService : Service() {
    private var activeDurationMinutes: Int = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        activeDurationMinutes = intent?.getIntExtra(EXTRA_DURATION_MINUTES, 0) ?: 0
        ensureChannel()
        startForeground(NOTIFICATION_ID, notification())
        return START_STICKY
    }

    private fun notification(): Notification {
        val duration = intentDurationText()
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("FutureFocus berjalan")
            .setContentText(duration)
            .setOngoing(true)
            .build()
    }

    private fun intentDurationText(): String {
        return if (activeDurationMinutes > 0) {
            "Sesi fokus $activeDurationMinutes menit sedang aktif."
        } else {
            "Sesi fokus sedang aktif."
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Focus Lock",
            NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        private const val CHANNEL_ID = "focus_lock_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
