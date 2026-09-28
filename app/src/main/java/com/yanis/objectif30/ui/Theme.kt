package com.yanis.objectif30.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFFC83D),
    onPrimary = Color(0xFF1A1608),
    secondary = Color(0xFF79AFFF),
    background = Color(0xFF0A0C10),
    surface = Color(0xFF141821),
    surfaceVariant = Color(0xFF1D2330),
    onBackground = Color(0xFFF3F5F7),
    onSurface = Color(0xFFF3F5F7)
)

@Composable
fun Objectif30Theme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
