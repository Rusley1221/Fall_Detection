package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.EmptyBox
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.ui.theme.LightBlue
import com.example.falldetection.ui.viewmodel.ContactsViewModel

@Composable
fun ContactsScreen(
    container: AppContainer,
    onOpenDetail: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val viewModel: ContactsViewModel = viewModel { ContactsViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Kontak Darurat", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang kontak")
            }
        }
        Spacer(Modifier.height(4.dp))
        Text("Kontak pertama (Seki) dipakai untuk pengujian SMS.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))

        when (state) {
            is AppResult.Loading -> com.example.falldetection.ui.components.LoadingBox("Memuat kontak…")
            is AppResult.Error -> ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
            is AppResult.Success -> {
                val contacts = (state as AppResult.Success).data
                if (contacts.isEmpty()) {
                    EmptyBox("Belum ada kontak darurat. Tekan Tambah Kontak.")
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(contacts, key = { it.id }) { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(contact.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                                            if (contact.is_primary) {
                                                Spacer(Modifier.width(8.dp))
                                                Surface(color = LightBlue, shape = MaterialTheme.shapes.medium) {
                                                    Text(
                                                        "Utama",
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                                        fontWeight = FontWeight.Bold,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                    )
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text("${contact.relationship} • ${contact.phone}", style = MaterialTheme.typography.bodyLarge)
                                    }
                                    IconButton(onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${contact.phone}")))
                                    }) {
                                        Icon(Icons.Default.Phone, contentDescription = "Telepon ${contact.name}", modifier = Modifier.size(28.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            text = "Tambah Kontak",
            onClick = onAdd,
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        )
    }
}
