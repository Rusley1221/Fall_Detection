package com.example.falldetection.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

@Composable
fun FallDetectionTheme(content: @Composable () -> Unit) {
    val colors = lightColorScheme(
        primary = PrimaryBlue,
        onPrimary = BackgroundWhite,
        primaryContainer = LightBlue,
        onPrimaryContainer = DarkBlue,
        secondary = DarkBlue,
        surface = BackgroundWhite,
        onSurface = TextPrimary,
        surfaceVariant = BackgroundAlt,
        onSurfaceVariant = TextSecondary,
        background = BackgroundWhite,
        onBackground = TextPrimary,
        error = Danger,
        onError = BackgroundWhite,
    )
    MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        shapes = MaterialTheme.shapes.copy(
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(20.dp),
        ),
        content = content,
    )
}
