package com.example.falldetection.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.LoadingBox
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.ContactFormViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ContactFormScreen(
    container: AppContainer,
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ContactFormViewModel = viewModel { ContactFormViewModel(id, container) }
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val saving = saved is AppResult.Loading
    val editable = !saving && !viewModel.loading && viewModel.loadError == null
    var discard by rememberSaveable { mutableStateOf(false) }
    val exit: () -> Unit = { if (viewModel.hasChanges()) discard = true else onBack() }
    BackHandler { if (!saving) exit() }
    if (discard) {
        AlertDialog(
            onDismissRequest = { discard = false },
            title = { Text("Buang perubahan?") },
            text = { Text("Perubahan kontak belum disimpan.") },
            confirmButton = { TextButton(onClick = onBack) { Text("Buang") } },
            dismissButton = { TextButton(onClick = { discard = false }) { Text("Lanjut edit") } },
        )
    }

    LaunchedEffect(saved) {
        if (saved is AppResult.Success) onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenTitle(if (id.isNullOrBlank() || id == "new") "Tambah Kontak" else "Ubah Kontak")
        Spacer(Modifier.height(16.dp))
        if (viewModel.loading) LoadingBox("Memuat kontak…")
        viewModel.loadError?.let {
            Text(it, color = Danger)
            TextButton(onClick = viewModel::loadContact) { Text("Coba lagi") }
        }

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = viewModel::onName,
            label = { Text("Nama kontak") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = editable,
            isError = viewModel.nameError != null,
            supportingText = { viewModel.nameError?.let { Text(it) } },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.phone,
            onValueChange = viewModel::onPhone,
            label = { Text("Nomor telepon") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = editable,
            isError = viewModel.phoneError != null,
            supportingText = { Text(viewModel.phoneError ?: "Contoh: 0812 3456 7890 atau +62 812 3456 7890") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next),
        )
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Anak", "Cucu", "Pasangan", "Tetangga").forEach { label ->
                FilterChip(
                    selected = viewModel.relationship.equals(label, ignoreCase = true),
                    onClick = { viewModel.onRelationship(label.lowercase()) },
                    label = { Text(label) },
                    enabled = editable,
                )
            }
        }
        OutlinedTextField(
            value = viewModel.relationship,
            onValueChange = viewModel::onRelationship,
            label = { Text("Hubungan") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            enabled = editable,
            isError = viewModel.relationshipError != null,
            supportingText = { Text(viewModel.relationshipError ?: "Pilih saran di atas atau isi sendiri.") },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.isPrimary, onCheckedChange = viewModel::onPrimary, enabled = editable)
            Text("Jadikan kontak utama", style = MaterialTheme.typography.bodyLarge)
        }

        Text("Kontak utama ditampilkan pertama. SMS tetap dikirim ke semua kontak darurat.", style = MaterialTheme.typography.bodyMedium)
        if (saved is AppResult.Error) {
            Spacer(Modifier.height(12.dp))
            Text((saved as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton(if (saving) "Menyimpan…" else "Simpan Kontak", enabled = editable, onClick = viewModel::save)
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Batal", enabled = !saving, onClick = exit)
    }
}
