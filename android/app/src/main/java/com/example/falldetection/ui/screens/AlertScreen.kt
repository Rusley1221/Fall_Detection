package com.example.falldetection.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.falldetection.detection.DetectionStateHolder
import com.example.falldetection.ui.components.PrimaryButton
import com.example.falldetection.ui.components.SecondaryButton
import com.example.falldetection.ui.theme.BackgroundWhite
import com.example.falldetection.ui.theme.Danger

@Composable
fun AlertScreen(onClose: (() -> Unit)? = null) {
    val alert by DetectionStateHolder.alert.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
            .background(Danger)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Text(
            "Apakah Anda terjatuh?",
            color = BackgroundWhite,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "${alert.secondsLeft}",
            color = BackgroundWhite,
            fontSize = 120.sp,
            fontWeight = FontWeight.Bold,
        )
        Text(
            "detik sebelum pesan darurat dikirim",
            color = BackgroundWhite,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Kekuatan benturan: ${String.format(java.util.Locale.US, "%.1f", alert.peakAcceleration)} m/s²",
            color = BackgroundWhite,
            style = MaterialTheme.typography.bodyLarge,
        )
        Spacer(Modifier.height(40.dp))
        PrimaryButton(
            text = "SAYA BAIK-BAIK SAJA",
            onClick = {
                DetectionStateHolder.cancel(context)
                onClose?.invoke()
            },
            color = BackgroundWhite,
            contentColor = Danger,
        )
        Spacer(Modifier.height(16.dp))
        SecondaryButton(
            text = "Minta Bantuan Sekarang",
            onClick = {
                DetectionStateHolder.helpNow(context)
                onClose?.invoke()
            },
            color = BackgroundWhite,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "Bila tidak dijawab, lokasi dan SMS darurat akan dikirim otomatis.",
            color = BackgroundWhite,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}
