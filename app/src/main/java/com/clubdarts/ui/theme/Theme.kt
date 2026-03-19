package com.clubdarts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Background,
    primaryContainer = AccentDim,
    onPrimaryContainer = Accent,
    secondary = TextSecondary,
    onSecondary = Background,
    secondaryContainer = Surface2,
    onSecondaryContainer = TextPrimary,
    tertiary = Blue,
    onTertiary = Background,
    background = Background,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = Border2,
    error = Red,
    onError = Color.White
)

@Composable
fun ClubDartsTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
