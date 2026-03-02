package com.example.primecastplayer.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val PrimeCastDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FBCFF),
    secondary = Color(0xFF4CE0C2),
    background = Color(0xFF101114),
    surface = Color(0xFF17191F),
    onPrimary = Color(0xFF031B3D),
    onSecondary = Color(0xFF00392E),
    onBackground = Color(0xFFE7EAF1),
    onSurface = Color(0xFFE7EAF1),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun PrimeCastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PrimeCastDarkColorScheme,
        typography = PrimeCastTypography,
        content = content
    )
}