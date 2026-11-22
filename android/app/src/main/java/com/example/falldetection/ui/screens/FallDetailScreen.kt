package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.components.StatusBadge
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.FallDetailViewModel
import com.example.falldetection.util.Format

@Composable
fun FallDetailScreen(container: AppContainer, id: String, onBack: () -> Unit) {
    val viewModel: FallDetailViewModel = viewModel { FallDetailViewModel(id, container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val action by viewModel.action.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus kejadian?") },
            text = { Text("Data ini akan dihapus dari server. Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.remove(onBack)
                }) { Text("Ya, Hapus", color = Danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Batal") }
            },
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenTitle("Detail Kejadian")
        Spacer(Modifier.height(16.dp))

        when (state) {
            is AppResult.Loading -> LoadingBox("Mengambil detail kejadian…")
            is AppResult.Error -> ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
            is AppResult.Success -> {
                val fall = (state as AppResult.Success).data
                SectionCard {
                    StatusBadge(fall.status)
                    Spacer(Modifier.height(12.dp))
                    Text("Waktu kejadian", style = MaterialTheme.typography.bodyMedium)
                    Text(Format.dateTime(fall.detected_at), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("Lokasi", style = MaterialTheme.typography.bodyMedium)
                    Text(Format.coordinates(fall.latitude, fall.longitude), style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    val url = Format.mapsUrl(fall.latitude, fall.longitude)
                    PrimaryButton(
                        text = "Buka di Google Maps",
                        onClick = {
                            if (url != null) {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                        },
                        enabled = url != null,
                    )
                }

                Spacer(Modifier.height(16.dp))
                SectionCard {
                    Text("Data sensor", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("Percepatan puncak: ${String.format(java.util.Locale.US, "%.1f", fall.peak_acceleration)} m/s²", style = MaterialTheme.typography.bodyLarge)
                    Text("Lama diam: ${fall.inactivity_seconds ?: 0.0} detik", style = MaterialTheme.typography.bodyLarge)
                    if (fall.location_accuracy != null) {
                        Text("Akurasi GPS: ${String.format(java.util.Locale.US, "%.1f", fall.location_accuracy)} m", style = MaterialTheme.typography.bodyLarge)
                    }
                    if (!fall.notes.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Catatan: ${fall.notes}", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(Modifier.height(20.dp))
                PrimaryButton("Tandai Sudah Ditangani", onClick = { viewModel.markResolved() })
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Hapus Kejadian") { confirmDelete = true }
            }
        }

        if (action is AppResult.Error) {
            Spacer(Modifier.height(12.dp))
            Text((action as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Kembali") { onBack() }
    }
}
