package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.InitialAvatar
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.components.SectionCard
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.ContactDetailViewModel
import com.example.falldetection.util.Format

@Composable
fun ContactDetailScreen(
    container: AppContainer,
    id: String,
    onEdit: (String) -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ContactDetailViewModel = viewModel { ContactDetailViewModel(id, container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deleted by viewModel.deleted.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val deleting = deleted is AppResult.Loading
    val contact = (state as? AppResult.Success)?.data
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted is AppResult.Success) onBack()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus kontak?") },
            text = {
                Text(when {
                    viewModel.totalContacts == 1 -> "Ini kontak darurat terakhir. Setelah dihapus, tidak ada penerima SMS darurat."
                    contact?.is_primary == true -> "${contact.name} adalah kontak utama. Anda bisa memilih kontak utama baru setelah menghapusnya."
                    else -> "${contact?.name ?: "Kontak ini"} tidak lagi menerima SMS darurat setelah dihapus."
                })
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.remove()
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
        ScreenTitle("Detail Kontak")
        Spacer(Modifier.height(16.dp))

        when (state) {
            is AppResult.Loading -> LoadingBox("Mengambil detail kontak…")
            is AppResult.Error -> ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
            is AppResult.Success -> {
                val contact = (state as AppResult.Success).data
                SectionCard {
                    Row {
                        InitialAvatar(contact.name)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                            if (contact.is_primary) {
                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.small) {
                                    Text("Kontak utama", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Hubungan", style = MaterialTheme.typography.bodyMedium)
                    Text(contact.relationship, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("Nomor telepon", style = MaterialTheme.typography.bodyMedium)
                    Text(contact.phone, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(Modifier.height(20.dp))
                val number = Format.phone(contact.phone)
                PrimaryButton("Telepon", enabled = !deleting && number.matches(Regex("[0-9]{8,15}"))) {
                    context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
                }
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Ubah Kontak", enabled = !deleting && !refreshing) { onEdit(id) }
                Spacer(Modifier.height(12.dp))
                SecondaryButton(if (deleting) "Menghapus…" else "Hapus Kontak", color = Danger, enabled = !deleting && !refreshing) { confirmDelete = true }
            }
        }

        if (deleted is AppResult.Error) {
            Spacer(Modifier.height(12.dp))
            Text((deleted as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Kembali", enabled = !deleting) { onBack() }
    }
}
