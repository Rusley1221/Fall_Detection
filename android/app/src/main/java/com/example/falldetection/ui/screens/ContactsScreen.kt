package com.example.falldetection.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.falldetection.data.AppResult
import com.example.falldetection.di.AppContainer
import com.example.falldetection.ui.components.EmptyBox
import com.example.falldetection.ui.components.InitialAvatar
import com.example.falldetection.ui.components.ErrorBox
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.ScreenTitle
import com.example.falldetection.util.Format
import com.example.falldetection.ui.viewmodel.ContactsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsScreen(
    container: AppContainer,
    onOpenDetail: (String) -> Unit,
    onAdd: () -> Unit,
) {
    val viewModel: ContactsViewModel = viewModel { ContactsViewModel(container) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val refreshError by viewModel.refreshError.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose { }
    }
    val context = LocalContext.current
    val contacts = (state as? AppResult.Success)?.data

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            ScreenTitle("Kontak Darurat", modifier = Modifier.weight(1f))
            IconButton(onClick = { viewModel.refresh() }, enabled = !refreshing) {
                Icon(Icons.Default.Refresh, contentDescription = "Muat ulang kontak")
            }
        }
        Spacer(Modifier.height(4.dp))
        contacts?.let { Text("${it.size} kontak darurat", style = MaterialTheme.typography.titleMedium) }
        Text("Jika SMS darurat aktif, pesan ditujukan ke semua kontak dalam daftar, bukan hanya kontak utama.", style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(12.dp))
        refreshError?.let {
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(12.dp)) {
                    Text(it, color = MaterialTheme.colorScheme.onErrorContainer)
                    TextButton(onClick = viewModel::refresh, enabled = !refreshing) { Text("Coba lagi") }
                }
            }
        }

        PullToRefreshBox(isRefreshing = refreshing && contacts != null, onRefresh = viewModel::refresh, modifier = Modifier.weight(1f)) {
        when (state) {
            is AppResult.Loading -> com.example.falldetection.ui.components.LoadingBox("Memuat kontak…")
            is AppResult.Error -> ErrorBox((state as AppResult.Error).message) { viewModel.refresh() }
            is AppResult.Success -> {
                val contacts = (state as AppResult.Success).data
                if (contacts.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(40.dp))
                            EmptyBox("Belum ada kontak darurat.")
                            TextButton(onClick = onAdd) { Text("Tambah kontak pertama") }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(contacts.sortedByDescending { it.is_primary }, key = { it.id }) { contact ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenDetail(contact.id) },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            ) {
                                Row(
                                    Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    InitialAvatar(contact.name, 40.dp)
                                    Spacer(Modifier.width(8.dp))
                                    val number = Format.phone(contact.phone)
                                    val validNumber = number.matches(Regex("[0-9]{8,15}"))
                                    Column(Modifier.weight(1f)) {
                                        Column {
                                            Text(contact.name, style = MaterialTheme.typography.titleMedium)
                                            if (contact.is_primary) {
                                                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.medium) {
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
                                        Text(contact.relationship, style = MaterialTheme.typography.bodyMedium)
                                        Text(contact.phone, style = MaterialTheme.typography.bodyLarge)
                                        if (!validNumber) Text("Nomor tidak valid", color = MaterialTheme.colorScheme.error)
                                    }
                                    IconButton(enabled = validNumber, onClick = {
                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", number, null)))
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
        }

        Spacer(Modifier.height(16.dp))
        if (contacts?.isEmpty() != true) PrimaryButton(
            text = "Tambah Kontak",
            onClick = onAdd,
            leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
        )
    }
}
