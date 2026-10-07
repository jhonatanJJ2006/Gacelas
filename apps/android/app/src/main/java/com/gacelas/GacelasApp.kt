package com.gacelas

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class GacelasApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // App-level initialization
    }
}
