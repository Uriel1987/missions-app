package com.ourhouse.missions.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val MissionsColorScheme = lightColorScheme(
    primary = AccentPrimary,
    onPrimary = Color.White,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    outline = Border,
    error = UrgentText
)

@Composable
fun MissionsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // TODO: add a dark palette if you want dark mode
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = MissionsColorScheme,
        typography = MissionsTypography,
        content = content
    )
}
