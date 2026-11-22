package com.example.falldetection.di

import android.content.Context
import androidx.work.WorkManager
import com.example.falldetection.data.local.AppDatabase
import com.example.falldetection.data.local.UserPreferences
import com.example.falldetection.data.remote.RetrofitClient
import com.example.falldetection.data.repository.ContactRepository
import com.example.falldetection.data.repository.FallRepository
import com.example.falldetection.data.repository.SmsRepository
import com.example.falldetection.data.repository.UserRepository
import com.example.falldetection.location.LocationProvider

/** Injeksi dependensi manual: satu pintu untuk seluruh repository. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val preferences = UserPreferences(appContext)
    val api = RetrofitClient.create(preferences)

    val userRepository = UserRepository(api)
    val contactRepository = ContactRepository(api, preferences)
    val fallRepository = FallRepository(
        api = api,
        dao = AppDatabase.get(appContext).pendingEventDao(),
        workManager = WorkManager.getInstance(appContext),
    )
    val smsRepository = SmsRepository(appContext, contactRepository, preferences)
    val locationProvider = LocationProvider(appContext)
}
