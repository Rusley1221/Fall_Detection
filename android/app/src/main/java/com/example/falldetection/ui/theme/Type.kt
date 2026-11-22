package com.example.falldetection.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    titleLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    titleMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary),
    bodyLarge = TextStyle(fontSize = 18.sp, color = TextPrimary),
    bodyMedium = TextStyle(fontSize = 16.sp, color = TextSecondary),
    labelLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),
    labelMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
)
