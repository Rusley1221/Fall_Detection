package com.example.falldetection.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.falldetection.BuildConfig
import com.example.falldetection.data.AppResult
import com.example.falldetection.detection.DetectionStateHolder
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.InitialAvatar
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.theme.Success
import com.example.falldetection.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(container: AppContainer) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(container) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val savedName by viewModel.savedName.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val savingSettings = saved is AppResult.Loading
    val savingName = savedName is AppResult.Loading
    val smsEnabled by viewModel.smsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val context = LocalContext.current
    var permissionRevision by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(context) {
        permissionRevision++
        onPauseOrDispose { }
    }
    val (locationGranted, smsGranted, notificationsGranted) = remember(context, permissionRevision) {
        fun granted(permission: String) =
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        Triple(
            granted(Manifest.permission.ACCESS_FINE_LOCATION) || granted(Manifest.permission.ACCESS_COARSE_LOCATION),
            granted(Manifest.permission.SEND_SMS),
            NotificationManagerCompat.from(context).areNotificationsEnabled(),
        )
    }
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { permissionRevision++ }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRevision++ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { permissionRevision++ }

    LaunchedEffect(saved) {
        if (saved is AppResult.Success) DetectionStateHolder.refreshSettings(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Profil", Modifier.weight(1f))
            TextButton(onClick = viewModel::refresh, enabled = !refreshing && !savingName && !savingSettings) {
                Text(if (refreshing) "Memuat…" else "Muat ulang")
            }
        }
        Spacer(Modifier.height(16.dp))

        SectionCard {
            when (user) {
                is AppResult.Loading -> LoadingBox("Memuat profil…")
                is AppResult.Error -> ErrorBox((user as AppResult.Error).message) { viewModel.refresh() }
                is AppResult.Success -> {
                    val data = (user as AppResult.Success).data
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        InitialAvatar(data.full_name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(data.full_name, style = MaterialTheme.typography.titleMedium)
                            Text("NIM: ${data.nim ?: "Belum diisi"}")
                            Text("Telepon: ${data.phone ?: "Belum diisi"}")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.editableName,
                        onValueChange = viewModel::onName,
                        label = { Text("Ubah nama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !refreshing && !savingName,
                        isError = viewModel.nameError != null,
                        supportingText = { viewModel.nameError?.let { Text(it) } },
                    )
                    SecondaryButton(
                        if (savingName) "Menyimpan nama…" else "Simpan Nama",
                        enabled = !savingName && !refreshing && viewModel.editableName.trim() != data.full_name,
                        onClick = viewModel::saveName,
                    )
                    SaveFeedback(savedName, "Nama berhasil disimpan.")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("Pengaturan deteksi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            when (val config = settings) {
                AppResult.Loading -> LoadingBox("Memuat pengaturan…")
                is AppResult.Error -> ErrorBox(config.message) { viewModel.refresh() }
                is AppResult.Success -> {
            Text("Sensitivitas", style = MaterialTheme.typography.bodyLarge)
            Text("Sensitivitas lebih tinggi merespons benturan yang lebih kecil. Demo hanya untuk pengujian.", style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                ProfileViewModel.IMPACT_LABELS.forEachIndexed { index, label ->
                    FilterChip(
                        selected = viewModel.impactIndex == index,
                        onClick = { viewModel.onImpact(index) },
                        label = { Text(label) },
                        enabled = !refreshing && !savingSettings,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Hitung mundur peringatan", style = MaterialTheme.typography.bodyLarge)
            Text("Waktu untuk membatalkan peringatan sebelum SMS darurat diproses.", style = MaterialTheme.typography.bodyMedium)
            FlowRow(
                horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
                verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
            ) {
                (ProfileViewModel.COUNTDOWNS + config.data.countdown_seconds).distinct().sorted().forEach { value ->
                    FilterChip(
                        selected = viewModel.countdown == value,
                        onClick = { viewModel.onCountdown(value) },
                        label = { Text("$value detik") },
                        enabled = !refreshing && !savingSettings,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Spacer(Modifier.height(20.dp))
            PrimaryButton(if (savingSettings) "Menyimpan…" else "Simpan Pengaturan Deteksi", enabled = !refreshing && !savingSettings) { viewModel.save() }
            SaveFeedback(saved, "Pengaturan deteksi berhasil disimpan.")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("SMS darurat", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Izinkan pengiriman SMS dari aplikasi", style = MaterialTheme.typography.bodyLarge)
                    Text("Perubahan ini langsung tersimpan di perangkat.", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = smsEnabled, onCheckedChange = viewModel::onSms)
            }
            if (smsEnabled && !smsGranted) Text("Aktifkan izin SMS di bawah agar pesan dapat diproses.", color = Danger)
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("Izin aplikasi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Berikan kembali izin bila sebelumnya ditolak agar lokasi dan SMS darurat tetap berfungsi.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(12.dp))
            PermissionRow("Lokasi", locationGranted) {
                locationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
            Spacer(Modifier.height(8.dp))
            PermissionRow("Notifikasi", notificationsGranted, onRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            } else null)
            PermissionRow("SMS darurat", smsGranted) {
                smsLauncher.launch(Manifest.permission.SEND_SMS)
            }
            if (!locationGranted || !smsGranted || !notificationsGranted) {
                SecondaryButton("Buka Pengaturan Aplikasi") {
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("Tentang Aplikasi", style = MaterialTheme.typography.titleMedium)
            Text("Fall Detection · versi ${BuildConfig.VERSION_NAME}")
            Spacer(Modifier.height(8.dp))
            Text("Mencatat kejadian jatuh dan membantu mengirim SMS darurat. Bukan alat medis atau pengganti layanan darurat resmi.", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(8.dp))
            Text("Pengembang: Syauqi Akifur Rahman · NIM 21120124130097", style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SaveFeedback(result: AppResult<Unit>?, message: String) {
    when (result) {
        is AppResult.Success -> Text(message, color = Success)
        is AppResult.Error -> Text(result.message, color = Danger)
        else -> Unit
    }
}

@Composable
private fun PermissionRow(label: String, granted: Boolean, onRequest: (() -> Unit)? = null) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(if (granted) "Diizinkan" else "Belum diizinkan", color = if (granted) Success else Danger)
        }
        if (!granted && onRequest != null) TextButton(onClick = onRequest) { Text("Izinkan") }
    }
}
