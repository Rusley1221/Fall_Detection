package com.example.falldetection.ui.screens

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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.viewmodel.ContactFormViewModel

@Composable
fun ContactFormScreen(
    container: AppContainer,
    id: String?,
    onDone: () -> Unit,
    onBack: () -> Unit,
) {
    val viewModel: ContactFormViewModel = viewModel { ContactFormViewModel(id, container) }
    val saved by viewModel.saved.collectAsStateWithLifecycle()

    LaunchedEffect(saved) {
        if (saved is AppResult.Success) onDone()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
    ) {
        ScreenTitle(if (id.isNullOrBlank() || id == "new") "Tambah Kontak" else "Ubah Kontak")
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = viewModel::onName,
            label = { Text("Nama kontak") },
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = viewModel.relationship,
            onValueChange = viewModel::onRelationship,
            label = { Text("Hubungan (anak, cucu, tetangga)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = viewModel.isPrimary, onCheckedChange = viewModel::onPrimary)
            Text("Jadikan kontak utama", style = MaterialTheme.typography.bodyLarge)
        }

        if (viewModel.error != null) {
            Spacer(Modifier.height(12.dp))
            Text(viewModel.error!!, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }
        if (saved is AppResult.Error) {
            Spacer(Modifier.height(12.dp))
            Text((saved as AppResult.Error).message, color = Danger, style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(Modifier.height(24.dp))
        PrimaryButton("Simpan Kontak") { viewModel.save(onDone) }
        Spacer(Modifier.height(12.dp))
        SecondaryButton("Batal") { onBack() }
    }
}
