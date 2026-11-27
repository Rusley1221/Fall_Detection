package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.data.remote.FallDto
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.StatusBadge
import com.example.falldetection.ui.components.SummaryTile
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.theme.TextSecondary
import com.example.falldetection.ui.theme.Warning
import com.example.falldetection.ui.viewmodel.HistoryViewModel
import com.example.falldetection.util.Format
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private data class HistoryRow(
    val id: String,
    val detectedAt: String,
    val status: String,
    val latitude: Double?,
    val longitude: Double?,
    val peak: Double,
    val pending: Boolean,
)

private sealed interface HistoryItem {
    data class Header(val label: String) : HistoryItem
    data class Event(val row: HistoryRow) : HistoryItem
}

private val jakartaZone: ZoneId = ZoneId.of("Asia/Jakarta")
private val dayFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("id", "ID"))
private val clockFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH.mm", Locale("id", "ID"))

private fun parseInstant(iso: String): Instant? =
    runCatching { Instant.parse(iso) }.getOrNull()
        ?: runCatching { java.time.OffsetDateTime.parse(iso).toInstant() }.getOrNull()

private fun dayLabel(instant: Instant?): String {
    if (instant == null) return "Tanggal tidak diketahui"
    val date = instant.atZone(jakartaZone).toLocalDate()
    val today = LocalDate.now(jakartaZone)
    return when (date) {
        today -> "Hari ini"
        today.minusDays(1) -> "Kemarin"
        else -> dayFormatter.format(date)
    }
}

private fun clockLabel(instant: Instant?): String =
    instant?.atZone(jakartaZone)?.format(clockFormatter) ?: "--.--"

private fun peakSeverity(peak: Double): String = when {
    peak >= 30 -> "sangat tinggi"
    peak >= 25 -> "tinggi"
    peak >= 20 -> "sedang"
    else -> "ringan"
}

private fun buildHistoryItems(rows: List<HistoryRow>): List<HistoryItem> {
    val sorted = rows.sortedByDescending { parseInstant(it.detectedAt) ?: Instant.EPOCH }
    val result = mutableListOf<HistoryItem>()
    var lastLabel: String? = null
    for (row in sorted) {
        val label = dayLabel(parseInstant(row.detectedAt))
        if (label != lastLabel) {
            result += HistoryItem.Header(label)
            lastLabel = label
        }
        result += HistoryItem.Event(row)
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HistoryScreen(container: AppContainer, onOpenDetail: (String) -> Unit) {
    val viewModel: HistoryViewModel = viewModel { HistoryViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    val pending by container.fallRepository.pending().collectAsStateWithLifecycle(emptyList())
    val context = LocalContext.current

    val allRows = buildList {
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
            (state as AppResult.Success<List<FallDto>>).data.forEach {
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
    }

    val rows = allRows.filter { row ->
        val filter = viewModel.filter
        filter == null || when (filter) {
            "confirmed" -> row.status == "confirmed"
            "cancelled" -> row.status == "cancelled"
            else -> true
        }
    }
    val historyItems = remember(rows) { buildHistoryItems(rows) }
    val errorMessage = refreshError ?: (state as? AppResult.Error)?.message

    val total = allRows.size
    val confirmed = allRows.count { it.status == "confirmed" }
    val cancelled = allRows.count { it.status == "cancelled" }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Riwayat Kejadian", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }, enabled = !refreshing) {
                if (refreshing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Refresh, contentDescription = "Muat ulang riwayat")
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
        ) {
            SummaryTile(
                "Total",
                total,
                MaterialTheme.colorScheme.primary,
                Modifier.weight(1f).fillMaxHeight(),
            )
            SummaryTile("Terkonfirmasi", confirmed, Danger, Modifier.weight(1f).fillMaxHeight())
            SummaryTile("Dibatalkan", cancelled, TextSecondary, Modifier.weight(1f).fillMaxHeight())
        }
        Spacer(Modifier.height(12.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
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

        if (refreshing) {
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        if (errorMessage != null && allRows.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Surface(
                color = Danger.copy(alpha = 0.10f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.refresh() }) { Text("Coba lagi") }
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f),
        ) {
            when {
                state is AppResult.Loading && allRows.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(12.dp))
                        Text("Sedang memuat data…", style = MaterialTheme.typography.bodyLarge)
                    }
                }

                state is AppResult.Error && allRows.isEmpty() -> {
                    ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
                }

                rows.isEmpty() -> {
                    HistoryEmpty(viewModel.filter) { viewModel.setFilter(null) }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = historyItems,
                            key = { item ->
                                when (item) {
                                    is HistoryItem.Header -> "header-${item.label}"
                                    is HistoryItem.Event -> item.row.id
                                }
                            },
                        ) { item ->
                            when (item) {
                                is HistoryItem.Header -> HistoryHeader(item.label)
                                is HistoryItem.Event -> HistoryCard(
                                    row = item.row,
                                    onOpen = {
                                        if (item.row.pending) {
                                            Toast.makeText(
                                                context,
                                                "Menunggu terkirim ke server",
                                                Toast.LENGTH_SHORT,
                                            ).show()
                                        } else {
                                            onOpenDetail(item.row.id)
                                        }
                                    },
                                    onOpenMap = { url ->
                                        context.startActivity(
                                            Intent(Intent.ACTION_VIEW, Uri.parse(url)),
                                        )
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun HistoryCard(
    row: HistoryRow,
    onOpen: () -> Unit,
    onOpenMap: (String) -> Unit,
) {
    val mapsUrl = Format.mapsUrl(row.latitude, row.longitude)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen),
        colors = CardDefaults.cardColors(
            containerColor = if (row.pending) {
                Warning.copy(alpha = 0.06f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = if (row.pending) BorderStroke(1.dp, Warning) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(clockLabel(parseInstant(row.detectedAt)), style = MaterialTheme.typography.titleMedium)
                Text(Format.date(row.detectedAt), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        Format.coordinates(row.latitude, row.longitude),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Speed,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Benturan ${String.format(Locale.US, "%.1f", row.peak)} m/s² " +
                            "(${peakSeverity(row.peak)})",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(row.status, row.pending)
                if (mapsUrl != null) {
                    Spacer(Modifier.height(4.dp))
                    IconButton(onClick = { onOpenMap(mapsUrl) }) {
                        Icon(
                            Icons.Default.Map,
                            contentDescription = "Buka lokasi di Google Maps",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryEmpty(filter: String?, onShowAll: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Default.History,
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            if (filter == null) {
                "Belum ada kejadian tercatat."
            } else {
                "Tidak ada kejadian pada filter ini."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
        if (filter != null) {
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onShowAll) { Text("Tampilkan semua") }
        }
    }
}
