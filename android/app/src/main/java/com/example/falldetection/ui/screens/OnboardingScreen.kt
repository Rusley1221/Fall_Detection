package com.example.falldetection.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.detection.DetectionStateHolder
import com.example.falldetection.ui.components.EmptyBox
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.viewmodel.OnboardingViewModel

@Composable
fun OnboardingScreen(container: AppContainer, onDone: () -> Unit) {
    val viewModel: OnboardingViewModel = viewModel { OnboardingViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var step by remember { mutableIntStateOf(0) }

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* Hasil dibaca ulang oleh LocationProvider saat dibutuhkan. */ }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }
    val smsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
    ) {
        ScreenTitle("Selamat datang di Fall Detection")
        Spacer(Modifier.height(16.dp))

        when (step) {
            0 -> {
                Text(
                    "Aplikasi ini membantu mendeteksi kemungkinan jatuh dan memberi tahu keluarga " +
                        "lewat SMS darurat yang berisi lokasi Anda.",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(16.dp))
                EmptyBox("Langkah 1 dari 3: kenalan dulu. Tekan Lanjut.")
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Lanjut") { step = 1 }
            }

            1 -> {
                Text("Isi data diri berikut (boleh dibantu keluarga).", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.name,
                    onValueChange = viewModel::onName,
                    label = { Text("Nama lengkap") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.nim,
                    onValueChange = viewModel::onNim,
                    label = { Text("NIM") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = viewModel.phone,
                    onValueChange = viewModel::onPhone,
                    label = { Text("Nomor telepon") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                Spacer(Modifier.height(20.dp))
                SecondaryButton("Kembali") { step = 0 }
                Spacer(Modifier.height(12.dp))
                PrimaryButton("Lanjut") { step = 2 }
            }

            else -> {
                Text("Langkah 3 dari 3: izin aplikasi.", style = androidx.compose.material3.MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(16.dp))
                EmptyBox(
                    "Lokasi dipakai agar keluarga tahu di mana Anda berada saat jatuh. " +
                        "Notifikasi menjaga pemantauan tetap berjalan. SMS dipakai untuk mengirim pesan darurat.",
                )
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Izinkan Lokasi") {
                    locationLauncher.launch(
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                    )
                }
                Spacer(Modifier.height(12.dp))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    SecondaryButton("Izinkan Notifikasi") {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                SecondaryButton("Izinkan SMS Darurat") {
                    smsLauncher.launch(Manifest.permission.SEND_SMS)
                }
                Spacer(Modifier.height(24.dp))
                when (state) {
                    is AppResult.Loading -> LoadingBox("Menyimpan profil ke server…")
                    is AppResult.Error -> {
                        if ((state as AppResult.Error).message.isNotBlank()) {
                            ErrorBox((state as AppResult.Error).message) { viewModel.submit(onDone) }
                            Spacer(Modifier.height(12.dp))
                        }
                    }

                    is AppResult.Success -> Unit
                }
                if (viewModel.fallbackMessage != null) {
                    Spacer(Modifier.height(12.dp))
                    Text(viewModel.fallbackMessage!!, style = androidx.compose.material3.MaterialTheme.typography.bodyMedium)
                }
                PrimaryButton("Selesai & Mulai") { viewModel.submit(onDone) }
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Kembali") { step = 1 }
            }
        }
    }
}

/** Dipanggil setelah onboarding bila pengguna ingin langsung mencoba. */
internal fun startDemoMonitoring(context: android.content.Context) {
    DetectionStateHolder.start(context)
}
