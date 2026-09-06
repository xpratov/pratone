package com.pratone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// Dark-mode only, per spec — no light color scheme is provided on purpose.
private val PratoneColorScheme = darkColorScheme(
    background = Background,
    surface = Surface,
    surfaceVariant = SurfaceElevated,
    primary = Accent,
    onPrimary = Background,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    secondary = TextSecondary,
    error = ErrorColor,
)

@Composable
fun PratoneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PratoneColorScheme,
        typography = PratoneTypography,
        content = content,
    )
}
