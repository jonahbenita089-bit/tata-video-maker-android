package com.tata.videomaker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TataDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8B5CF6),
    secondary = Color(0xFF22D3EE),
    tertiary = Color(0xFFEC4899),
    background = Color(0xFF090B14),
    surface = Color(0xFF111827),
    surfaceVariant = Color(0xFF1F2937),
    onPrimary = Color.White,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    outline = Color(0xFF2F3A4D)
)

@Composable
fun TataVideoMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TataDarkColorScheme,
        typography = TataTypography,
        content = content
    )
}
