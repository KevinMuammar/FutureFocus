package com.example.futurefocus

import android.app.Application
import com.example.futurefocus.data.local.AppDatabase
import com.example.futurefocus.utils.NetworkMonitor
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings

class FutureFocusApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }
    val networkMonitor: NetworkMonitor by lazy { NetworkMonitor(this) }

    companion object {
        lateinit var instance: FutureFocusApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        val settings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(
                com.google.firebase.firestore.PersistentCacheSettings.newBuilder()
                    .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                    .build()
            )
            .build()
        FirebaseFirestore.getInstance().firestoreSettings = settings

        networkMonitor.register()
    }

    override fun onTerminate() {
        networkMonitor.unregister()
        super.onTerminate()
    }
}
