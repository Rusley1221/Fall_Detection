package com.example.falldetection.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.detection.DetectionStateHolder
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(container: AppContainer) {
    val viewModel: ProfileViewModel = viewModel { ProfileViewModel(container) }
    val user by viewModel.user.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val smsEnabled by viewModel.smsEnabled.collectAsStateWithLifecycle(initialValue = true)
    val context = LocalContext.current
    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(saved) {
        if (saved is AppResult.Success) DetectionStateHolder.refreshSettings(context)
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenTitle("Profil & Tentang Aplikasi")
        Spacer(Modifier.height(16.dp))

        SectionCard {
            when (user) {
                is AppResult.Loading -> LoadingBox("Memuat profil…")
                is AppResult.Error -> ErrorBox((user as AppResult.Error).message) { viewModel.refresh() }
                is AppResult.Success -> {
                    val data = (user as AppResult.Success).data
                    Text("Nama", style = MaterialTheme.typography.bodyMedium)
                    Text(data.full_name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("NIM", style = MaterialTheme.typography.bodyMedium)
                    Text(data.nim ?: "-", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = viewModel.editableName,
                        onValueChange = viewModel::onName,
                        label = { Text("Ubah nama") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("Pengaturan deteksi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            if (settings is AppResult.Loading) LoadingBox("Memuat pengaturan…")
            Text("Sensitivitas", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ProfileViewModel.IMPACT_LABELS.forEachIndexed { index, label ->
                    FilterChip(
                        selected = viewModel.impactIndex == index,
                        onClick = { viewModel.onImpact(index) },
                        label = { Text(label) },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("Hitung mundur peringatan", style = MaterialTheme.typography.bodyLarge)
            Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)) {
                ProfileViewModel.COUNTDOWNS.forEach { value ->
                    FilterChip(
                        selected = viewModel.countdown == value,
                        onClick = { viewModel.onCountdown(value) },
                        label = { Text("$value detik") },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(checked = smsEnabled, onCheckedChange = viewModel::onSms)
                Spacer(Modifier.height(0.dp))
                Text(" Kirim SMS darurat otomatis", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(Modifier.height(20.dp))
            PrimaryButton("Simpan Pengaturan") { viewModel.save() }
            if (saved is AppResult.Error) {
                Spacer(Modifier.height(8.dp))
                Text((saved as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
            }
        }

        Spacer(Modifier.height(16.dp))
        SectionCard {
            Text("Tentang Aplikasi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text("Fall Detection versi 1.0", style = MaterialTheme.typography.bodyLarge)
            Text(
                "Aplikasi ini membantu mencatat kejadian jatuh beserta lokasinya dan mengirim " +
                    "SMS darurat ke kontak yang tersedia. Aplikasi ini BUKAN alat medis dan bukan " +
                    "pengganti layanan darurat resmi.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(12.dp))
            Text("Pengembang", style = MaterialTheme.typography.bodyMedium)
            Text("Syauqi Akifur Rahman", style = MaterialTheme.typography.bodyLarge)
            Text("NIM 21120124130097", style = MaterialTheme.typography.bodyLarge)
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
            PrimaryButton("Izinkan Lokasi") {
                locationLauncher.launch(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                SecondaryButton("Izinkan Notifikasi") {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Spacer(Modifier.height(8.dp))
            }
            SecondaryButton("Izinkan SMS Darurat") {
                smsLauncher.launch(Manifest.permission.SEND_SMS)
            }
        }

        Spacer(Modifier.height(24.dp))
        SecondaryButton("Muat Ulang Profil") { viewModel.refresh() }
    }
}
