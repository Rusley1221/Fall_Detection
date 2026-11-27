package com.example.falldetection.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.falldetection.ui.theme.Danger
import com.example.falldetection.ui.theme.Success
import com.example.falldetection.ui.theme.TextPrimary
import com.example.falldetection.ui.theme.TextSecondary
import com.example.falldetection.ui.theme.Warning
import com.example.falldetection.util.Format

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = modifier)
}

@Composable
fun PrimaryButton(
    text: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    leadingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = contentColor),
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = color)
    }
}

@Composable
fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(modifier = Modifier.padding(20.dp), content = content)
    }
}

@Composable
fun StatusBadge(status: String, pending: Boolean = false) {
    val label: String
    val color: Color
    val icon: ImageVector
    when {
        pending -> {
            label = "Menunggu terkirim"
            color = Warning
            icon = Icons.Default.Refresh
        }
        status == "confirmed" -> {
            label = "Terkonfirmasi"
            color = Danger
            icon = Icons.Default.Warning
        }
        status == "cancelled" -> {
            label = "Dibatalkan"
            color = TextSecondary
            icon = Icons.Default.Close
        }
        status == "resolved" -> {
            label = "Sudah ditangani"
            color = Success
            icon = Icons.Default.Check
        }
        else -> {
            label = Format.statusLabel(status)
            color = TextSecondary
            icon = Icons.Default.Check
        }
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.medium) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(label, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = MaterialTheme.typography.bodyMedium.fontSize)
        }
    }
}

@Composable
fun LoadingBox(text: String = "Sedang memuat data…") {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun ErrorBox(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Default.Warning, contentDescription = null, tint = Danger, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(message, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(16.dp))
        PrimaryButton("Coba Lagi", modifier = Modifier.fillMaxWidth(0.7f), onClick = onRetry)
    }
}

@Composable
fun EmptyBox(message: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

/** Tile angka ringkas: dipakai Beranda dan Riwayat agar tampilan konsisten. */
@Composable
fun SummaryTile(
    label: String,
    value: Int,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, color = color.copy(alpha = 0.10f), shape = MaterialTheme.shapes.medium) {
        Column(
            modifier = Modifier.fillMaxSize().padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("$value", style = MaterialTheme.typography.titleMedium, color = color)
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** Avatar bulat berisi huruf awal nama; dipakai kartu kontak dan profil. */
@Composable
fun InitialAvatar(name: String, size: Dp = 48.dp, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(size),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = CircleShape,
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = name.trim().take(1).ifBlank { "?" }.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
