package com.example.falldetection.ui.screens

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.falldetection.ui.components.EmptyBox
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.StatusBadge
import com.example.falldetection.ui.viewmodel.HistoryViewModel
import com.example.falldetection.util.Format

private data class HistoryRow(
    val id: String,
    val detectedAt: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val peak: Double,
    val pending: Boolean,
)

@Composable
fun HistoryScreen(container: AppContainer, onOpenDetail: (String) -> Unit) {
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pending by container.fallRepository.pending().collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    val rows = buildList {
        pending.filter { !it.synced }.forEach {
            add(
                HistoryRow(
                    id = it.id,
                    detectedAt = it.detected_at,
                    status = it.status,
                    latitude = it.latitude,
                    longitude = it.longitude,
                    peak = it.peak_acceleration,
                    pending = true,
                ),
            )
        }
        if (state is AppResult.Success) {
            (state as AppResult.Success<List<com.example.falldetection.data.remote.FallDto>>).data.forEach {
                add(
                    HistoryRow(
                        id = it.id,
                        detectedAt = it.detected_at,
                        status = it.status,
                        latitude = it.latitude,
                        longitude = it.longitude,
                        peak = it.peak_acceleration,
                        pending = false,
                    ),
                )
            }
        }
    }.filter { row ->
        val filter = viewModel.filter
        filter == null || when (filter) {
            "confirmed" -> row.status == "confirmed"
            "cancelled" -> row.status == "cancelled"
            else -> true
        }
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Riwayat Kejadian", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang riwayat")
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = viewModel.filter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("Semua") },
            )
            FilterChip(
                selected = viewModel.filter == "confirmed",
                onClick = { viewModel.setFilter("confirmed") },
                label = { Text("Terkonfirmasi") },
            )
            FilterChip(
                selected = viewModel.filter == "cancelled",
                onClick = { viewModel.setFilter("cancelled") },
                label = { Text("Dibatalkan") },
            )
        }
        Spacer(Modifier.height(12.dp))

        when {
            state is AppResult.Loading && rows.isEmpty() -> {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("Sedang memuat data…", style = MaterialTheme.typography.bodyLarge)
                }
            }

            state is AppResult.Error && rows.isEmpty() -> ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
            rows.isEmpty() -> EmptyBox("Belum ada kejadian pada filter ini.")
            else -> LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(rows, key = { it.id }) { row ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (row.pending) {
                                    Toast.makeText(context, "Menunggu terkirim ke server", Toast.LENGTH_SHORT).show()
                                } else {
                                    onOpenDetail(row.id)
                                }
                            },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            StatusBadge(row.status, row.pending)
                            Spacer(Modifier.height(8.dp))
                            Text(Format.dateTime(row.detectedAt), style = MaterialTheme.typography.bodyLarge)
                            Spacer(Modifier.height(4.dp))
                            Text(Format.coordinates(row.latitude, row.longitude), style = MaterialTheme.typography.bodyMedium)
                            Text("Percepatan puncak: ${String.format(java.util.Locale.US, "%.1f", row.peak)} m/s²", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
