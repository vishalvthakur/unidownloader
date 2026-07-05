package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = ElegantPrimary,
    onPrimary = ElegantOnPrimary,
    secondary = ElegantAccent,
    background = ElegantBackground,
    surface = ElegantCard,
    onBackground = ElegantTextLight,
    onSurface = ElegantTextLight,
    surfaceVariant = ElegantSecondaryCard,
    onSurfaceVariant = ElegantTextMuted,
    outline = ElegantOutline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = DarkColorScheme // Lock premium Elegant Dark for an immersive experience

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
