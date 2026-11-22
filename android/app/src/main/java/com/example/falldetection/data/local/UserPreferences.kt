package com.example.falldetection.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.falldetection.data.remote.ContactDto
import com.example.falldetection.util.AppJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "fall_detection")

class UserPreferences(private val context: Context) {
    companion object {
        private val KEY_USER_ID = stringPreferencesKey("user_id")
        private val KEY_ONBOARDING = booleanPreferencesKey("onboarding_done")
        private val KEY_SMS_ENABLED = booleanPreferencesKey("sms_enabled")
        private val KEY_CACHED_CONTACTS = stringPreferencesKey("cached_contacts")
        const val DEFAULT_NAME = "Syauqi Akifur Rahman"
        const val DEFAULT_NIM = "21120124130097"
        /** User demo yang sudah memiliki Seki + decoy di seed API. Dipakai bila API offline saat onboarding. */
        const val DEFAULT_USER_ID = "11111111-1111-1111-1111-111111111111"
    }

    val userId: Flow<String?> = context.dataStore.data.map { it[KEY_USER_ID] }
    val onboardingDone: Flow<Boolean> = context.dataStore.data.map { it[KEY_ONBOARDING] == true }
    val smsEnabled: Flow<Boolean> = context.dataStore.data.map { it[KEY_SMS_ENABLED] != false }

    suspend fun getUserIdOnce(): String? = context.dataStore.data.first()[KEY_USER_ID]
    suspend fun setUserId(value: String) {
        context.dataStore.edit { it[KEY_USER_ID] = value }
    }

    suspend fun isOnboardingDone(): Boolean = context.dataStore.data.first()[KEY_ONBOARDING] == true
    suspend fun setOnboardingDone(value: Boolean) {
        context.dataStore.edit { it[KEY_ONBOARDING] = value }
    }

    suspend fun isSmsEnabled(): Boolean = context.dataStore.data.first()[KEY_SMS_ENABLED] != false
    suspend fun setSmsEnabled(value: Boolean) {
        context.dataStore.edit { it[KEY_SMS_ENABLED] = value }
    }

    suspend fun getCachedContacts(): List<ContactDto> {
        val raw = context.dataStore.data.first()[KEY_CACHED_CONTACTS] ?: return emptyList()
        return try {
            AppJson.json.decodeFromString(raw)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveCachedContacts(contacts: List<ContactDto>) {
        context.dataStore.edit { it[KEY_CACHED_CONTACTS] = AppJson.json.encodeToString(contacts) }
    }
}
