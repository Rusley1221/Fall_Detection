package com.example.falldetection.ui.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.data.remote.FallDto
import com.example.falldetection.di.AppContainer
import com.example.falldetection.detection.DetectionStateHolder
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.components.StatusBadge
import com.example.falldetection.ui.components.SummaryTile
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.theme.Success
import com.example.falldetection.ui.theme.TextSecondary
import com.example.falldetection.ui.theme.Warning
import com.example.falldetection.ui.viewmodel.HomeSummary
import com.example.falldetection.ui.viewmodel.HomeViewModel
import com.example.falldetection.util.Format
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    container: AppContainer,
    onOpenHistory: () -> Unit = {},
    onOpenContacts: () -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel { HomeViewModel(container) }
    val summary by viewModel.summary.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    val monitoring by DetectionStateHolder.monitoring.collectAsStateWithLifecycle()
    val smsStatus by DetectionStateHolder.smsStatus.collectAsStateWithLifecycle()
    val lastEvent by DetectionStateHolder.lastEvent.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val data = (summary as? AppResult.Success)?.data
    val errorMessage = refreshError ?: (summary as? AppResult.Error)?.message
    val latestFall = lastEvent ?: data?.lastFall

    var confirmHelp by remember { mutableStateOf(false) }
    var missing by remember { mutableStateOf(checkMissingPermissions(context)) }
    var firstResume by remember { mutableStateOf(true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                missing = checkMissingPermissions(context)
                // ON_RESUME pertama terjadi bersamaan dengan muat awal; lewati agar tidak ganda.
                if (firstResume) firstResume = false else viewModel.refresh()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Setelah ada kejadian baru, angka ringkasan ikut diperbarui tanpa menunggu refresh manual.
    val lastEventId = lastEvent?.id
    LaunchedEffect(lastEventId) {
        if (lastEventId != null) viewModel.refresh()
    }

    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ScreenTitle("Beranda")
                    Text(
                        "Pemantauan jatuh dan status darurat",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                IconButton(onClick = { viewModel.refresh() }, enabled = !refreshing) {
                    if (refreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Muat ulang beranda")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            MonitoringCard(
                monitoring = monitoring,
                data = data,
                onToggle = {
                    if (monitoring) {
                        DetectionStateHolder.stop(context)
                    } else {
                        DetectionStateHolder.start(context)
                    }
                },
            )

            if (errorMessage != null && data != null) {
                Spacer(Modifier.height(12.dp))
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

            if (missing.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                PermissionWarning(missing) {
                    context.startActivity(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            Uri.fromParts("package", context.packageName, null),
                        ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Text("Ringkasan", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            when (summary) {
                is AppResult.Loading -> SectionCard { LoadingBox() }
                is AppResult.Error -> SectionCard {
                    ErrorBox((summary as AppResult.Error).message) { viewModel.refresh() }
                }

                is AppResult.Success -> Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    SummaryTile(
                        "Total",
                        data?.totalFalls ?: 0,
                        MaterialTheme.colorScheme.primary,
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                    SummaryTile(
                        "Terkonfirmasi",
                        data?.confirmedFalls ?: 0,
                        Danger,
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                    SummaryTile(
                        "Kontak",
                        data?.contactCount ?: 0,
                        Success,
                        Modifier.weight(1f).fillMaxHeight(),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            ContactCard(data = data, onOpenContacts = onOpenContacts, onCall = { phone ->
                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Format.phone(phone)}")))
            })

            Spacer(Modifier.height(16.dp))
            LastEventCard(
                fall = latestFall,
                showAll = (data?.totalFalls ?: 0) > 0,
                onOpenHistory = onOpenHistory,
                onOpenMap = { url ->
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
            )

            if (smsStatus.isNotBlank()) {
                Spacer(Modifier.height(12.dp))
                SmsStatusCard(smsStatus) { DetectionStateHolder.clearSmsStatus() }
            }

            Spacer(Modifier.height(16.dp))
            TestModeCard(
                countdown = data?.countdownSeconds,
                monitoring = monitoring,
                onSimulate = {
                    if (!monitoring) DetectionStateHolder.start(context)
                    DetectionStateHolder.simulate(context)
                },
                onHelp = { confirmHelp = true },
            )

            Spacer(Modifier.height(24.dp))
        }
    }

    if (confirmHelp) {
        val total = data?.contactCount ?: 0
        AlertDialog(
            onDismissRequest = { confirmHelp = false },
            title = { Text("Kirim bantuan sekarang?") },
            text = {
                Text(
                    if (total > 0) {
                        "SMS darurat akan langsung dikirim ke $total kontak tanpa menunggu hitung mundur."
                    } else {
                        "SMS darurat akan langsung dikirim ke semua kontak tanpa menunggu hitung mundur."
                    },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmHelp = false
                        DetectionStateHolder.helpNow(context)
                    },
                ) { Text("Ya, kirim") }
            },
            dismissButton = {
                TextButton(onClick = { confirmHelp = false }) { Text("Batal") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonitoringCard(monitoring: Boolean, data: HomeSummary?, onToggle: () -> Unit) {
    val accent = if (monitoring) Success else Danger
    val sensitivity = Format.sensitivityLabel(data?.impactThreshold)

    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = accent.copy(alpha = 0.18f), shape = CircleShape) {
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (monitoring) Icons.Default.Check else Icons.Default.Close,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (monitoring) "Pemantauan AKTIF" else "Pemantauan NONAKTIF",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        if (monitoring) "Sensor gerak sedang berjalan" else "Sensor gerak berhenti",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                if (monitoring) {
                    "Jika terdeteksi jatuh, peringatan berbunyi dan SMS darurat dikirim setelah hitung mundur."
                } else {
                    "Aktifkan pemantauan agar aplikasi dapat mendeteksi jatuh dan mengirim SMS darurat."
                },
                style = MaterialTheme.typography.bodyLarge,
            )

            if (data != null) {
                Spacer(Modifier.height(12.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (sensitivity != null) InfoPill("Sensitivitas $sensitivity", accent)
                    data.countdownSeconds?.let { InfoPill("Hitung mundur $it dtk", accent) }
                    InfoPill("${data.contactCount} kontak darurat", accent)
                }
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                text = if (monitoring) "Nonaktifkan Pemantauan" else "Aktifkan Pemantauan",
                onClick = onToggle,
                color = if (monitoring) MaterialTheme.colorScheme.primary else Success,
            )
        }
    }
}

@Composable
private fun InfoPill(text: String, color: Color) {
    Surface(color = color.copy(alpha = 0.14f), shape = MaterialTheme.shapes.small) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PermissionWarning(missing: List<String>, onOpenSettings: () -> Unit) {
    Surface(
        color = Warning.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, Warning.copy(alpha = 0.40f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = Warning)
                Spacer(Modifier.width(8.dp))
                Text("Izin belum lengkap", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Tanpa izin ${missing.joinToString(", ") { it.lowercase() }}, deteksi atau pengiriman " +
                    "SMS darurat bisa gagal.",
                style = MaterialTheme.typography.bodyMedium,
            )
            TextButton(onClick = onOpenSettings) { Text("Buka pengaturan aplikasi") }
        }
    }
}

@Composable
private fun ContactCard(
    data: HomeSummary?,
    onOpenContacts: () -> Unit,
    onCall: (String) -> Unit,
) {
    val name = data?.primaryContact
    val phone = data?.primaryPhone
    val detail = listOfNotNull(data?.primaryRelationship, phone).joinToString(" · ")

    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape = CircleShape) {
                Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                    Text(
                        (name ?: "?").take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name ?: "Belum ada kontak darurat",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    detail.ifBlank { "Tambahkan kontak agar SMS darurat bisa dikirim." },
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(
                text = "Telepon",
                modifier = Modifier.weight(1f),
                enabled = phone != null,
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                onClick = { phone?.let(onCall) },
            )
            SecondaryButton(
                text = "Kelola",
                modifier = Modifier.weight(1f),
                onClick = onOpenContacts,
            )
        }
    }
}

@Composable
private fun LastEventCard(
    fall: FallDto?,
    showAll: Boolean,
    onOpenHistory: () -> Unit,
    onOpenMap: (String) -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Kejadian terakhir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (showAll) {
                TextButton(onClick = onOpenHistory) { Text("Lihat semua") }
            }
        }

        if (fall == null) {
            Spacer(Modifier.height(4.dp))
            Text("Belum ada kejadian tercatat.", style = MaterialTheme.typography.bodyLarge)
            return@SectionCard
        }

        Spacer(Modifier.height(8.dp))
        StatusBadge(fall.status)
        Spacer(Modifier.height(10.dp))
        Text(Format.dateTime(fall.detected_at), style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Speed, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                String.format(Locale.US, "Benturan %.1f m/s²", fall.peak_acceleration),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                Format.coordinates(fall.latitude, fall.longitude),
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        val mapsUrl = Format.mapsUrl(fall.latitude, fall.longitude)
        if (mapsUrl != null) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton("Buka di Google Maps", onClick = { onOpenMap(mapsUrl) })
        }
    }
}

@Composable
private fun SmsStatusCard(message: String, onDismiss: () -> Unit) {
    val problem = listOf("tidak ada sms", "gagal", "tidak terkirim", "belum")
        .any { message.contains(it, ignoreCase = true) }
    val accent = if (problem) Warning else Success

    Surface(
        color = accent.copy(alpha = 0.10f),
        shape = MaterialTheme.shapes.large,
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = if (problem) Icons.Default.Warning else Icons.Default.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f).padding(vertical = 6.dp),
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Tutup status SMS", tint = TextSecondary)
            }
        }
    }
}

@Composable
private fun TestModeCard(
    countdown: Int?,
    monitoring: Boolean,
    onSimulate: () -> Unit,
    onHelp: () -> Unit,
) {
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Mode Uji", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            InfoPill("Khusus demo", Warning)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Simulasi memicu hitung mundur ${countdown ?: 20} detik sehingga masih bisa dibatalkan. " +
                "Bantuan langsung mengirim SMS tanpa menunggu.",
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = "Simulasi Jatuh",
            color = Warning,
            onClick = onSimulate,
        )
        Spacer(Modifier.height(8.dp))
        SecondaryButton(
            text = "Minta Bantuan Sekarang",
            color = Danger,
            onClick = onHelp,
        )
        if (!monitoring) {
            Spacer(Modifier.height(8.dp))
            Text(
                "Pemantauan akan diaktifkan otomatis saat tombol ditekan.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

private fun checkMissingPermissions(context: Context): List<String> {
    val missing = mutableListOf<String>()
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) !=
        PackageManager.PERMISSION_GRANTED
    ) {
        missing += "SMS"
    }
    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) {
        missing += "Lokasi"
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
        !NotificationManagerCompat.from(context).areNotificationsEnabled()
    ) {
        missing += "Notifikasi"
    }
    return missing
}
