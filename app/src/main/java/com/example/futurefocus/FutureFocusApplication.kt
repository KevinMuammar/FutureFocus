package com.example.futurefocus

import android.app.Application
import com.example.futurefocus.data.local.AppDatabase

class FutureFocusApplication : Application() {
    val database: AppDatabase by lazy { AppDatabase.getInstance(this) }

    companion object {
        lateinit var instance: FutureFocusApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
