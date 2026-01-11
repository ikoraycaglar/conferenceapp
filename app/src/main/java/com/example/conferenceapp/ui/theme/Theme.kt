package com.example.conferenceapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A5AE0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE7E0FF),
    onPrimaryContainer = Color(0xFF1B1145),

    secondary = Color(0xFF00BFA6),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB2F1E6),
    onSecondaryContainer = Color(0xFF00201B),

    tertiary = Color(0xFFFFB300),
    onTertiary = Color(0xFF1E1B00),
    tertiaryContainer = Color(0xFFFFE08A),
    onTertiaryContainer = Color(0xFF1E1B00),

    background = Color(0xFFF8F7FF),
    onBackground = Color(0xFF1B1B1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE6E1EC),
    onSurfaceVariant = Color(0xFF49454F),

    error = Color(0xFFB00020),
    onError = Color.White
)

@Composable
fun ConferenceTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = Typography,
        content = content
    )
}