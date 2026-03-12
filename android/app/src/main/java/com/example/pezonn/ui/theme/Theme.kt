package com.example.pezonn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val MetroColorScheme = darkColorScheme(
    primary = MetroCyan,
    secondary = MetroGold,
    background = MetroBackground,
    surface = MetroSurface,
    onPrimary = MetroTextPrimary,
    onSecondary = MetroBackground,
    onBackground = MetroTextPrimary,
    onSurface = MetroTextPrimary,
)

@Composable
fun PezonnTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MetroColorScheme,
        typography = Typography,
        content = content
    )
}
