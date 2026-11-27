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

data class HomeSummary(
    val totalFalls: Int = 0,
    val confirmedFalls: Int = 0,
    val cancelledFalls: Int = 0,
    val contactCount: Int = 0,
    val primaryContact: String? = null,
    val primaryPhone: String? = null,
    val primaryRelationship: String? = null,
    val impactThreshold: Double? = null,
    val countdownSeconds: Int? = null,
    val lastFall: FallDto? = null,
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {
    private val _summary = MutableStateFlow<AppResult<HomeSummary>>(AppResult.Loading)
    val summary: StateFlow<AppResult<HomeSummary>> = _summary.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val hasData = _summary.value is AppResult.Success
            if (hasData) {
                _refreshing.value = true
            } else {
                _summary.value = AppResult.Loading
            }

            val falls = container.fallRepository.list()
            val contacts = container.contactRepository.list()
            // Pengaturan hanya pelengkap: gagal memuatnya tidak boleh menyembunyikan data utama.
            val settings = (container.userRepository.settings() as? AppResult.Success)?.data

            when {
                falls is AppResult.Success && contacts is AppResult.Success -> {
                    val primary = contacts.data.firstOrNull { it.is_primary }
                        ?: contacts.data.firstOrNull()
                    _summary.value = AppResult.Success(
                        HomeSummary(
                            totalFalls = falls.data.size,
                            confirmedFalls = falls.data.count { it.status == "confirmed" },
                            cancelledFalls = falls.data.count { it.status == "cancelled" },
                            contactCount = contacts.data.size,
                            primaryContact = primary?.name,
                            primaryPhone = primary?.phone,
                            primaryRelationship = primary?.relationship,
                            impactThreshold = settings?.impact_threshold,
                            countdownSeconds = settings?.countdown_seconds,
                            lastFall = latestFall(falls.data),
                        ),
                    )
                    _refreshError.value = null
                }

                falls is AppResult.Error -> fail(falls.message, hasData)
                contacts is AppResult.Error -> fail(contacts.message, hasData)
                else -> fail("Data tidak dapat dimuat", hasData)
            }
            _refreshing.value = false
        }
    }

    private fun fail(message: String, hasData: Boolean) {
        if (hasData) {
            // Data lama tetap ditampilkan; kesalahan dilaporkan lewat banner.
            _refreshError.value = message
        } else {
            _summary.value = AppResult.Error(message)
        }
    }

    private fun latestFall(falls: List<FallDto>): FallDto? = falls.maxByOrNull { fall ->
        runCatching {
            java.time.OffsetDateTime.parse(fall.detected_at).toInstant().toEpochMilli()
        }.getOrDefault(0L)
    }
}

class HistoryViewModel(private val container: AppContainer) : ViewModel() {
    // Setter publik diberi nama lain agar tidak bentrok dengan fungsi setFilter().
    @set:JvmName("internalSetFilter")
    var filter by mutableStateOf<String?>(null)
        private set

    private val _state = MutableStateFlow<AppResult<List<FallDto>>>(AppResult.Loading)
    val state: StateFlow<AppResult<List<FallDto>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    init { refresh() }

    /** Filter diterapkan lokal supaya daftar tidak berkedip/loading setiap ganti chip. */
    fun setFilter(value: String?) {
        filter = value
    }

    fun refresh() {
        viewModelScope.launch {
            val hasData = _state.value is AppResult.Success
            if (hasData) {
                _refreshing.value = true
            } else {
                _state.value = AppResult.Loading
            }
            when (val result = container.fallRepository.list()) {
                is AppResult.Success -> {
                    _state.value = result
                    _refreshError.value = null
                }

                is AppResult.Error -> {
                    if (hasData) {
                        _refreshError.value = result.message
                    } else {
                        _state.value = result
                    }
                }

                AppResult.Loading -> Unit
            }
            _refreshing.value = false
        }
    }
}

class ContactsViewModel(private val container: AppContainer) : ViewModel() {
    private val _state = MutableStateFlow<AppResult<List<ContactDto>>>(AppResult.Loading)
    val state: StateFlow<AppResult<List<ContactDto>>> = _state.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    private val _refreshError = MutableStateFlow<String?>(null)
    val refreshError: StateFlow<String?> = _refreshError.asStateFlow()

    /**
     * Dipanggil layar setiap kali tampil lagi (termasuk saat kembali dari form/detail)
     * supaya daftar tidak basi. Data lama dipertahankan selama proses refresh.
     */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            val hasData = _state.value is AppResult.Success
            _refreshing.value = true
            if (!hasData) _state.value = AppResult.Loading
            when (val result = container.contactRepository.list()) {
                is AppResult.Success -> {
                    _state.value = result
                    _refreshError.value = null
                }

                is AppResult.Error -> {
                    if (hasData) {
                        _refreshError.value = result.message
                    } else {
                        _state.value = result
                    }
                }

                AppResult.Loading -> Unit
            }
            _refreshing.value = false
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

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /** Jumlah kontak terakhir yang diketahui; dipakai untuk memperingatkan saat menghapus kontak terakhir. */
    var totalContacts by mutableStateOf<Int?>(null)
        private set

    /** Dimuat ulang setiap layar tampil agar perubahan dari form langsung terlihat. */
    fun refresh() {
        if (_refreshing.value) return
        viewModelScope.launch {
            _refreshing.value = true
            _state.value = container.contactRepository.detail(id)
            totalContacts = (container.contactRepository.list() as? AppResult.Success)?.data?.size
            _refreshing.value = false
        }
    }

    fun remove() {
        if (_deleted.value is AppResult.Loading) return
        viewModelScope.launch {
            _deleted.value = AppResult.Loading
            _deleted.value = container.contactRepository.delete(id)
        }
    }
}

class ContactFormViewModel(
    private val id: String?,
    private val container: AppContainer,
) : ViewModel() {
    private data class Snapshot(
        val name: String,
        val phone: String,
        val relationship: String,
        val primary: Boolean,
    )

    var name by mutableStateOf("")
        private set
    var phone by mutableStateOf("")
        private set
    var relationship by mutableStateOf("anak")
        private set
    var isPrimary by mutableStateOf(false)
        private set

    var loading by mutableStateOf(false)
        private set
    var loadError by mutableStateOf<String?>(null)
        private set
    var nameError by mutableStateOf<String?>(null)
        private set
    var phoneError by mutableStateOf<String?>(null)
        private set
    var relationshipError by mutableStateOf<String?>(null)
        private set

    private var initial = Snapshot("", "", "anak", false)

    private val _saved = MutableStateFlow<AppResult<Unit>?>(null)
    val saved: StateFlow<AppResult<Unit>?> = _saved.asStateFlow()

    init {
        loadContact()
    }

    fun loadContact() {
        if (!loading && !id.isNullOrBlank() && id != "new") {
            loading = true
            loadError = null
            viewModelScope.launch {
                when (val result = container.contactRepository.detail(id)) {
                    is AppResult.Success -> {
                        name = result.data.name
                        phone = result.data.phone
                        relationship = result.data.relationship
                        isPrimary = result.data.is_primary
                        initial = Snapshot(name, phone, relationship, isPrimary)
                    }

                    is AppResult.Error -> loadError = result.message
                    AppResult.Loading -> Unit
                }
                loading = false
            }
        }
    }

    /** Dipakai untuk konfirmasi sebelum membuang perubahan yang belum disimpan. */
    fun hasChanges(): Boolean =
        name != initial.name ||
            phone != initial.phone ||
            relationship != initial.relationship ||
            isPrimary != initial.primary

    fun onName(value: String) {
        name = value
        nameError = null
        clearSavedError()
    }

    fun onPhone(value: String) {
        phone = value
        phoneError = null
        clearSavedError()
    }

    fun onRelationship(value: String) {
        relationship = value
        relationshipError = null
        clearSavedError()
    }

    fun onPrimary(value: Boolean) {
        isPrimary = value
        clearSavedError()
    }

    private fun clearSavedError() {
        if (_saved.value is AppResult.Error) _saved.value = null
    }

    fun save() {
        if (_saved.value is AppResult.Loading || loading || loadError != null) return

        val normalized = Format.phone(phone)
        nameError = when {
            name.isBlank() -> "Nama kontak wajib diisi"
            name.trim().length > 80 -> "Nama kontak maksimal 80 karakter"
            else -> null
        }
        phoneError = when {
            !phone.trim().matches(Regex("\\+?[0-9 ()-]+")) && phone.isNotBlank() -> "Gunakan angka dan format telepon yang valid"
            normalized.length < 8 -> "Nomor telepon minimal 8 digit"
            normalized.length > 15 -> "Nomor telepon maksimal 15 digit"
            else -> null
        }
        relationshipError = if (relationship.trim().isBlank()) "Hubungan wajib diisi" else null
        if (nameError != null || phoneError != null || relationshipError != null) return

        val request = ContactRequest(name.trim(), normalized, relationship.trim(), isPrimary)
        viewModelScope.launch {
            _saved.value = AppResult.Loading
            val contacts = container.contactRepository.list()
            if (contacts is AppResult.Success && contacts.data.any {
                    it.id != id && Format.phone(it.phone) == normalized
                }) {
                phoneError = "Nomor ini sudah ada di kontak darurat"
                _saved.value = null
                return@launch
            }
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
    var impactIndex by mutableStateOf(1) // 0 Rendah, 1 Sedang, 2 Tinggi, 3 Demo
        private set
    var countdown by mutableStateOf(20)
        private set
    var nameError by mutableStateOf<String?>(null)
        private set

    private val _user = MutableStateFlow<AppResult<UserDto>>(AppResult.Loading)
    val user: StateFlow<AppResult<UserDto>> = _user.asStateFlow()

    private val _settings = MutableStateFlow<AppResult<SettingsDto>>(AppResult.Loading)
    val settings: StateFlow<AppResult<SettingsDto>> = _settings.asStateFlow()

    /** Hasil simpan pengaturan deteksi. */
    private val _saved = MutableStateFlow<AppResult<Unit>?>(null)
    val saved: StateFlow<AppResult<Unit>?> = _saved.asStateFlow()

    /** Hasil simpan nama, terpisah supaya tombolnya tidak ambigu. */
    private val _savedName = MutableStateFlow<AppResult<Unit>?>(null)
    val savedName: StateFlow<AppResult<Unit>?> = _savedName.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    val smsEnabled = container.preferences.smsEnabled

    init { refresh() }

    private fun applySettings(data: SettingsDto) {
        val threshold = data.impact_threshold
        impactIndex = when {
            threshold >= 28 -> 0
            threshold >= 23 -> 1
            threshold >= 18 -> 2
            else -> 3
        }
        countdown = data.countdown_seconds
    }

    fun onName(value: String) {
        editableName = value
        nameError = null
        if (_savedName.value is AppResult.Success) _savedName.value = null
    }

    fun onImpact(index: Int) {
        impactIndex = index
        if (_saved.value is AppResult.Success) _saved.value = null
    }

    fun onCountdown(value: Int) {
        countdown = value
        if (_saved.value is AppResult.Success) _saved.value = null
    }

    fun onSms(value: Boolean) {
        viewModelScope.launch { container.preferences.setSmsEnabled(value) }
    }

    /** Nama disimpan terpisah agar tombol "Simpan Pengaturan" tidak ikut mengubah nama. */
    fun saveName() {
        if (_savedName.value is AppResult.Loading || _refreshing.value) return
        val current = (_user.value as? AppResult.Success)?.data ?: return
        val name = editableName.trim()
        if (name.isBlank()) {
            nameError = "Nama tidak boleh kosong"
            return
        }
        nameError = null
        viewModelScope.launch {
            _savedName.value = AppResult.Loading
            val result = container.userRepository.updateProfile(
                name,
                current.phone,
                current.nim,
            )
            _savedName.value = when (result) {
                is AppResult.Success -> {
                    _user.value = result
                    editableName = result.data.full_name
                    AppResult.Success(Unit)
                }

                is AppResult.Error -> result
                AppResult.Loading -> AppResult.Error("Terjadi kesalahan")
            }
        }
    }

    fun save() {
        if (_saved.value is AppResult.Loading || _refreshing.value) return
        val current = (_settings.value as? AppResult.Success)?.data ?: return
        viewModelScope.launch {
            _saved.value = AppResult.Loading
            val threshold = if (impactIndex == sensitivityIndex(current.impact_threshold)) current.impact_threshold else when (impactIndex) {
                0 -> 30.0
                1 -> 25.0
                2 -> 20.0
                else -> 15.0
            }
            _saved.value = when (val result = container.userRepository.saveSettings(threshold, current.inactivity_seconds, countdown)) {
                is AppResult.Success -> {
                    _settings.value = result
                    AppResult.Success(Unit)
                }

                is AppResult.Error -> result
                AppResult.Loading -> AppResult.Error("Terjadi kesalahan")
            }
        }
    }

    fun refresh() {
        if (_refreshing.value || _saved.value is AppResult.Loading || _savedName.value is AppResult.Loading) return
        viewModelScope.launch {
            _refreshing.value = true
            val oldProfile = (_user.value as? AppResult.Success)?.data
            val profile = container.userRepository.profile()
            _user.value = profile
            if (profile is AppResult.Success) {
                if (oldProfile == null || editableName == oldProfile.full_name) editableName = profile.data.full_name
                nameError = null
            }
            val settings = container.userRepository.settings()
            _settings.value = settings
            if (settings is AppResult.Success) applySettings(settings.data)
            _refreshing.value = false
        }
    }

    companion object {
        private fun sensitivityIndex(threshold: Double): Int = when {
            threshold >= 28 -> 0
            threshold >= 23 -> 1
            threshold >= 18 -> 2
            else -> 3
        }
        val IMPACT_LABELS = listOf("Rendah", "Sedang", "Tinggi", "Demo")
        val COUNTDOWNS = listOf(15, 20, 30)
    }
}
