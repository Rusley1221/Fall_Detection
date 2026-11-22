package com.example.falldetection.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.falldetection.data.AppResult
import com.example.falldetection.data.remote.ContactDto
import com.example.falldetection.data.remote.ContactRequest
import com.example.falldetection.data.remote.FallDto
import com.example.falldetection.data.remote.SettingsDto
import com.example.falldetection.data.remote.UserDto
import com.example.falldetection.di.AppContainer
import com.example.falldetection.util.Format
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(private val container: AppContainer) : ViewModel() {
    var name by mutableStateOf(com.example.falldetection.data.local.UserPreferences.DEFAULT_NAME)
        private set
    var phone by mutableStateOf("082176119857")
        private set
    var nim by mutableStateOf(com.example.falldetection.data.local.UserPreferences.DEFAULT_NIM)
        private set
    var fallbackMessage by mutableStateOf<String?>(null)
        private set

    private val _state = MutableStateFlow<AppResult<Unit>>(AppResult.Error(""))
    val state: StateFlow<AppResult<Unit>> = _state.asStateFlow()

    fun onName(value: String) { name = value }
    fun onPhone(value: String) { phone = value }
    fun onNim(value: String) { nim = value }

    fun submit(onDone: () -> Unit) {
        viewModelScope.launch {
            _state.value = AppResult.Loading
            val result = container.userRepository.createProfile(
                name.trim(),
                phone.trim().ifBlank { null },
                nim.trim().ifBlank { null },
            )
            _state.value = when (result) {
                is AppResult.Success -> {
                    container.preferences.setUserId(result.data.id)
                    container.preferences.setOnboardingDone(true)
                    AppResult.Success(Unit)
                }

                is AppResult.Error -> {
                    // API boleh mati saat first-run: tetap masuk memakai user demo yang
                    // sudah memiliki Seki + decoy di seed. Profil bisa disinkronkan nanti.
                    fallbackMessage =
                        "Server belum bisa dihubungi. Aplikasi memakai profil demo Syauqi Akifur Rahman; " +
                            "nyalakan API untuk data lengkap."
                    container.preferences.setUserId(
                        com.example.falldetection.data.local.UserPreferences.DEFAULT_USER_ID,
                    )
                    container.preferences.setOnboardingDone(true)
                    AppResult.Success(Unit)
                }

                AppResult.Loading -> AppResult.Error("Terjadi kesalahan")
            }
            if (_state.value is AppResult.Success) onDone()
        }
    }
}

data class HomeSummary(val totalFalls: Int, val primaryContact: String?, val primaryPhone: String?)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _summary = MutableStateFlow<AppResult<HomeSummary>>(AppResult.Loading)
    val summary: StateFlow<AppResult<HomeSummary>> = _summary.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _summary.value = AppResult.Loading
            val falls = container.fallRepository.list()
            val contacts = container.contactRepository.list()
            _summary.value = when {
                falls is AppResult.Success && contacts is AppResult.Success -> AppResult.Success(
                    HomeSummary(
                        totalFalls = falls.data.size,
                        primaryContact = contacts.data.firstOrNull { it.is_primary }?.name
                            ?: contacts.data.firstOrNull()?.name,
                        primaryPhone = contacts.data.firstOrNull { it.is_primary }?.phone
                            ?: contacts.data.firstOrNull()?.phone,
                    ),
                )

                falls is AppResult.Error -> AppResult.Error(falls.message)
                contacts is AppResult.Error -> AppResult.Error(contacts.message)
                else -> AppResult.Error("Data tidak dapat dimuat")
            }
        }
    }
}

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    var filter by mutableStateOf<String?>(null)
        private set

    private val _state = MutableStateFlow<AppResult<List<FallDto>>>(AppResult.Loading)
    val state: StateFlow<AppResult<List<FallDto>>> = _state.asStateFlow()

    init { refresh() }

    @JvmName("updateFilter")
    fun setFilter(value: String?) {
        filter = value
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AppResult.Loading
            _state.value = container.fallRepository.list(filter)
        }
    }
}

class ContactsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<AppResult<List<ContactDto>>>(AppResult.Loading)
    val state: StateFlow<AppResult<List<ContactDto>>> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = AppResult.Loading
            _state.value = container.contactRepository.list()
        }
    }
}

class ContactDetailViewModel(
    private val id: String,
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow<AppResult<ContactDto>>(AppResult.Loading)
    val state: StateFlow<AppResult<ContactDto>> = _state.asStateFlow()

    private val _deleted = MutableStateFlow<AppResult<Unit>?>(null)
    val deleted: StateFlow<AppResult<Unit>?> = _deleted.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _state.value = container.contactRepository.detail(id) }
    }

    fun remove() {
        viewModelScope.launch { _deleted.value = container.contactRepository.delete(id) }
    }
}

class ContactFormViewModel(
    private val id: String?,
    private val container: AppContainer,
) : ViewModel() {
    var name by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var relationship by mutableStateOf("anak")
        private set
    var isPrimary by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set

    private val _saved = MutableStateFlow<AppResult<Unit>?>(null)
    val saved: StateFlow<AppResult<Unit>?> = _saved.asStateFlow()

    init {
        if (!id.isNullOrBlank() && id != "new") {
            viewModelScope.launch {
                when (val result = container.contactRepository.detail(id)) {
                    is AppResult.Success -> {
                        name = result.data.name
                        phone = result.data.phone
                        relationship = result.data.relationship
                        isPrimary = result.data.is_primary
                    }

                    is AppResult.Error -> error = result.message
                    AppResult.Loading -> Unit
                }
            }
        }
    }

    fun onName(value: String) { name = value }
    fun onPhone(value: String) { phone = value }
    fun onRelationship(value: String) { relationship = value }
    fun onPrimary(value: Boolean) { isPrimary = value }

    fun save(onDone: () -> Unit) {
        val request = ContactRequest(name.trim(), Format.phone(phone), relationship.trim(), isPrimary)
        error = when {
            request.name.isBlank() -> "Nama kontak wajib diisi"
            request.phone.length < 8 -> "Nomor telepon minimal 8 digit"
            request.relationship.isBlank() -> "Hubungan wajib diisi"
            else -> null
        }
        if (error != null) return

        viewModelScope.launch {
            _saved.value = AppResult.Loading
            val result = if (id.isNullOrBlank() || id == "new") {
                container.contactRepository.create(request)
            } else {
                container.contactRepository.update(id, request)
            }
            _saved.value = when (result) {
                is AppResult.Success -> AppResult.Success(Unit)
                is AppResult.Error -> result
                AppResult.Loading -> AppResult.Error("Terjadi kesalahan")
            }
            if (_saved.value is AppResult.Success) onDone()
        }
    }
}

class FallDetailViewModel(
    private val id: String,
    private val container: AppContainer,
) : ViewModel() {
    private val _state = MutableStateFlow<AppResult<FallDto>>(AppResult.Loading)
    val state: StateFlow<AppResult<FallDto>> = _state.asStateFlow()

    private val _action = MutableStateFlow<AppResult<Unit>?>(null)
    val action: StateFlow<AppResult<Unit>?> = _action.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch { _state.value = container.fallRepository.detail(id) }
    }

    fun markResolved() {
        viewModelScope.launch {
            _action.value = AppResult.Loading
            _action.value = when (val result = container.fallRepository.patch(id, "resolved")) {
                is AppResult.Success -> {
                    _state.value = AppResult.Success(result.data)
                    AppResult.Success(Unit)
                }

                is AppResult.Error -> result
                AppResult.Loading -> AppResult.Error("Terjadi kesalahan")
            }
        }
    }

    fun remove(onDone: () -> Unit) {
        viewModelScope.launch {
            val result = container.fallRepository.delete(id)
            if (result is AppResult.Error) _action.value = result else onDone()
        }
    }
}

class ProfileViewModel(private val container: AppContainer) : ViewModel() {
    var editableName by mutableStateOf("")
        private set
    var impactIndex by mutableStateOf(1) // 0 Rendah, 1 Sedang, 2 Tinggi
        private set
    var countdown by mutableStateOf(20)
        private set

    private val _user = MutableStateFlow<AppResult<UserDto>>(AppResult.Loading)
    val user: StateFlow<AppResult<UserDto>> = _user.asStateFlow()

    private val _settings = MutableStateFlow<AppResult<SettingsDto>>(AppResult.Loading)
    val settings: StateFlow<AppResult<SettingsDto>> = _settings.asStateFlow()

    private val _saved = MutableStateFlow<AppResult<Unit>?>(null)
    val saved: StateFlow<AppResult<Unit>?> = _saved.asStateFlow()

    val smsEnabled = container.preferences.smsEnabled

    init {
        viewModelScope.launch {
            _user.value = container.userRepository.profile()
            if (_user.value is AppResult.Success) {
                editableName = (_user.value as AppResult.Success<UserDto>).data.full_name
            }
            val result = container.userRepository.settings()
            _settings.value = result
            if (result is AppResult.Success) {
                val threshold = result.data.impact_threshold
                impactIndex = when {
                    threshold >= 28 -> 0
                    threshold >= 23 -> 1
                    threshold >= 18 -> 2
                    else -> 3
                }
                countdown = result.data.countdown_seconds
            }
        }
    }

    fun onName(value: String) { editableName = value }
    fun onImpact(index: Int) { impactIndex = index }
    fun onCountdown(value: Int) { countdown = value }
    fun onSms(value: Boolean) {
        viewModelScope.launch { container.preferences.setSmsEnabled(value) }
    }

    fun save() {
        viewModelScope.launch {
            _saved.value = AppResult.Loading
            val threshold = when (impactIndex) { 0 -> 30.0; 1 -> 25.0; 2 -> 20.0; else -> 15.0 }
            val currentPhone = (_user.value as? AppResult.Success<UserDto>)?.data?.phone
            val profile = container.userRepository.updateProfile(
                editableName.trim().ifBlank { "Syauqi Akifur Rahman" },
                currentPhone,
                com.example.falldetection.data.local.UserPreferences.DEFAULT_NIM,
            )
            val config = container.userRepository.saveSettings(threshold, 3, countdown)
            _saved.value = if (profile is AppResult.Success && config is AppResult.Success) {
                _settings.value = AppResult.Success(config.data)
                if (profile is AppResult.Success) _user.value = AppResult.Success(profile.data)
                AppResult.Success(Unit)
            } else {
                AppResult.Error(
                    (profile as? AppResult.Error)?.message ?: (config as? AppResult.Error)?.message
                    ?: "Gagal menyimpan pengaturan",
                )
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { _user.value = container.userRepository.profile() }
    }

    companion object {
        val IMPACT_LABELS = listOf("Rendah", "Sedang", "Tinggi", "Demo")
        val COUNTDOWNS = listOf(15, 20, 30)
    }
}
