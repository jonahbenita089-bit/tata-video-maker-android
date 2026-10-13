package com.tata.videomaker.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TataDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00B7FF),
    secondary = Color(0xFF00D5A3),
    background = Color(0xFF050505),
    surface = Color(0xFF111111),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White,
    outline = Color(0xFF2A2A2A)
)

@Composable
fun TataVideoMakerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TataDarkColorScheme,
        content = content
    )
}
