package com.example.supermetrics.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF00363D),
    onPrimaryContainer = NeonCyan,
    secondary = NeonGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF00391A),
    onSecondaryContainer = NeonGreen,
    tertiary = NeonAmber,
    onTertiary = Color.Black,
    background = DarkBackground,
    onBackground = Color(0xFFEEEEEE),
    surface = DarkSurface,
    onSurface = Color(0xFFE6E6E6),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C4D0),
    outline = DarkBorder,
    error = NeonRed,
    onError = Color.Black
)

@Composable
fun SuperMetricsTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // La aplicación utiliza exclusivamente Dark Mode en la paleta de colores de Material3
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}