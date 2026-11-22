package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.components.StatusBadge
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.theme.Success
import com.example.falldetection.ui.viewmodel.HomeViewModel
import com.example.falldetection.util.Format

@Composable
fun HomeScreen(container: AppContainer) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container) }
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val monitoring by DetectionStateHolder.monitoring.collectAsStateWithLifecycle()
    val smsStatus by DetectionStateHolder.smsStatus.collectAsStateWithLifecycle()
    val lastEvent by DetectionStateHolder.lastEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenTitle("Beranda / Monitor")
        Spacer(Modifier.height(16.dp))

        SectionCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (monitoring) Icons.Default.Check else Icons.Default.Close,
                    contentDescription = null,
                    tint = if (monitoring) Success else Danger,
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    if (monitoring) "Pemantauan AKTIF" else "Pemantauan NONAKTIF",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(12.dp))
            Text(
                if (monitoring) {
                    "Aplikasi sedang mendengarkan gerakan. Biarkan notifikasi tetap tampil."
                } else {
                    "Tekan tombol di bawah untuk mulai mendeteksi jatuh."
                },
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                text = if (monitoring) "Nonaktifkan Pemantauan" else "Aktifkan Pemantauan",
                onClick = {
                    if (monitoring) DetectionStateHolder.stop(context) else DetectionStateHolder.start(context)
                },
                color = if (monitoring) MaterialTheme.colorScheme.primary else Success,
            )
        }

        Spacer(Modifier.height(20.dp))
        ScreenTitle("Ringkasan", modifier = Modifier)
        Spacer(Modifier.height(8.dp))
        SectionCard {
            when (summary) {
                is AppResult.Loading -> LoadingBox()
                is AppResult.Error -> ErrorBox((summary as AppResult.Error).message) { viewModel.refresh() }
                is AppResult.Success -> {
                    val data = (summary as AppResult.Success).data
                    Text("Total kejadian tercatat: ${data.totalFalls}", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Kontak utama: ${data.primaryContact ?: "belum ada"}",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Spacer(Modifier.height(16.dp))
                    val phone = data.primaryPhone
                    PrimaryButton(
                        text = "Hubungi Kontak Utama",
                        onClick = {
                            if (phone != null) {
                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                            }
                        },
                        enabled = phone != null,
                    )
                }
            }
        }

        if (lastEvent != null) {
            Spacer(Modifier.height(20.dp))
            SectionCard {
                Text("Kejadian terakhir", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                StatusBadge(lastEvent!!.status)
                Spacer(Modifier.height(8.dp))
                Text(Format.dateTime(lastEvent!!.detected_at), style = MaterialTheme.typography.bodyLarge)
                Text(Format.coordinates(lastEvent!!.latitude, lastEvent!!.longitude), style = MaterialTheme.typography.bodyMedium)
            }
        }

        if (smsStatus.isNotBlank()) {
            Spacer(Modifier.height(16.dp))
            SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Danger)
                    Spacer(Modifier.width(8.dp))
                    Text(smsStatus, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("Mode Uji (untuk demo)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        EmptyBox("Tekan tombol ini untuk mensimulasikan jatuh tanpa menjatuhkan perangkat.")
        PrimaryButton("Simulasi Jatuh") {
            if (!monitoring) DetectionStateHolder.start(context)
            DetectionStateHolder.simulate(context)
        }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Minta Bantuan Sekarang") { DetectionStateHolder.helpNow(context) }
        Spacer(Modifier.height(32.dp))
    }
}
