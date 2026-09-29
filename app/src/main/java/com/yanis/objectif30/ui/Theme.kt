package com.yanis.objectif30.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val WildsportColors = darkColorScheme(
    primary = Color(0xFF08E7F0),
    onPrimary = Color(0xFF00171B),
    primaryContainer = Color(0xFF083B47),
    onPrimaryContainer = Color(0xFFB9FAFF),
    secondary = Color(0xFF4EA7FF),
    onSecondary = Color(0xFF00182A),
    secondaryContainer = Color(0xFF0D2A49),
    onSecondaryContainer = Color(0xFFD5E8FF),
    tertiary = Color(0xFF8B7CFF),
    background = Color(0xFF05090E),
    surface = Color(0xFF0A1118),
    surfaceVariant = Color(0xFF101C26),
    onBackground = Color(0xFFEAF7FA),
    onSurface = Color(0xFFEAF7FA),
    onSurfaceVariant = Color(0xFFAFC4CB),
    outline = Color(0xFF284653),
    error = Color(0xFFFF7A8A)
)

@Composable
fun Objectif30Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WildsportColors,
        content = content
    )
}
