package com.example.falldetection.data.repository

import com.example.falldetection.data.AppResult
import com.example.falldetection.data.callApi
import com.example.falldetection.data.callApiUnit
import com.example.falldetection.data.local.UserPreferences
import com.example.falldetection.data.remote.ApiService
import com.example.falldetection.data.remote.ContactDto
import com.example.falldetection.data.remote.ContactRequest

class ContactRepository(
    private val api: ApiService,
    private val preferences: UserPreferences,
) {
    suspend fun list(): AppResult<List<ContactDto>> = when (val result = callApi { api.getContacts() }) {
        is AppResult.Success -> {
            preferences.saveCachedContacts(result.data)
            result
        }
        else -> result
    }

    suspend fun cached(): List<ContactDto> = preferences.getCachedContacts()

    suspend fun detail(id: String): AppResult<ContactDto> = callApi { api.getContact(id) }

    suspend fun create(request: ContactRequest): AppResult<ContactDto> = when (val result = callApi { api.createContact(request) }) {
        is AppResult.Success -> {
            refreshCache()
            result
        }
        else -> result
    }

    suspend fun update(id: String, request: ContactRequest): AppResult<ContactDto> =
        when (val result = callApi { api.updateContact(id, request) }) {
            is AppResult.Success -> {
                refreshCache()
                result
            }
            else -> result
        }

    suspend fun delete(id: String): AppResult<Unit> = when (val result = callApiUnit { api.deleteContact(id) }) {
        is AppResult.Success -> {
            refreshCache()
            result
        }
        else -> result
    }

    private suspend fun refreshCache() {
        when (val result = callApi { api.getContacts() }) {
            is AppResult.Success -> preferences.saveCachedContacts(result.data)
            else -> Unit
        }
    }
}
