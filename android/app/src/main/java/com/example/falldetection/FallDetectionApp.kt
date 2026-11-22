package com.example.falldetection

import android.app.Application
import com.example.falldetection.di.AppContainer

class FallDetectionApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
