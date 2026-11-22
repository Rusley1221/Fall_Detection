package com.example.falldetection.ui.screens

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
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
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.ContactDetailViewModel

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
    val context = LocalContext.current
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(deleted) {
        if (deleted is AppResult.Success) onBack()
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Hapus kontak?") },
            text = { Text("Kontak darurat ini akan dihapus dari server.") },
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
                    Text("Nama", style = MaterialTheme.typography.bodyMedium)
                    Text(contact.name, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(12.dp))
                    Text("Hubungan", style = MaterialTheme.typography.bodyMedium)
                    Text(contact.relationship, style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(12.dp))
                    Text("Nomor telepon", style = MaterialTheme.typography.bodyMedium)
                    Text(contact.phone, style = MaterialTheme.typography.bodyLarge)
                    if (contact.is_primary) {
                        Spacer(Modifier.height(12.dp))
                        Text("Kontak utama", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton("Telepon", onClick = {
                    context.startActivity(Intent(Intent.ACTION_DIAL, "tel:${contact.phone}".toUri()))
                })
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Ubah Kontak", onClick = { onEdit(id) })
                Spacer(Modifier.height(12.dp))
                SecondaryButton("Hapus Kontak", onClick = { confirmDelete = true })
            }
        }

        if (deleted is AppResult.Error) {
            Spacer(Modifier.height(12.dp))
            Text((deleted as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }
        Spacer(Modifier.height(24.dp))
        SecondaryButton("Kembali", onClick = { onBack() })
    }
}
