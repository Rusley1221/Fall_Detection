package com.example.falldetection.data.repository

import com.example.falldetection.data.AppResult
import com.example.falldetection.data.callApi
import com.example.falldetection.data.remote.ApiService
import com.example.falldetection.data.remote.CreateUserRequest
import com.example.falldetection.data.remote.SettingsDto
import com.example.falldetection.data.remote.UpdateSettingsRequest
import com.example.falldetection.data.remote.UpdateUserRequest
import com.example.falldetection.data.remote.UserDto

class UserRepository(private val api: ApiService) {
    suspend fun createProfile(name: String, phone: String?, nim: String?): AppResult<UserDto> =
        callApi { api.createUser(CreateUserRequest(name, phone, nim)) }

    suspend fun profile(): AppResult<UserDto> = callApi { api.getUser() }

    suspend fun updateProfile(name: String, phone: String?, nim: String?): AppResult<UserDto> =
        callApi { api.updateUser(UpdateUserRequest(name, phone, nim)) }

    suspend fun settings(): AppResult<SettingsDto> = callApi { api.getSettings() }

    suspend fun saveSettings(
        impactThreshold: Double,
        inactivitySeconds: Int,
        countdownSeconds: Int,
    ): AppResult<SettingsDto> = callApi {
        api.updateSettings(UpdateSettingsRequest(impactThreshold, inactivitySeconds, countdownSeconds))
    }

    /** Dipakai service saat start; null bila server tidak tersedia. */
    suspend fun settingsOnce(): SettingsDto? = when (val result = settings()) {
        is AppResult.Success -> result.data
        else -> null
    }
}
