package com.yanis.objectif30.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val WildsportColors = darkColorScheme(
    primary = WildCyan,
    onPrimary = Color(0xFF001317),
    primaryContainer = Color(0xFF123642),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = WildBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF17334E),
    onSecondaryContainer = Color(0xFFFFFFFF),
    tertiary = WildViolet,
    onTertiary = Color.White,
    background = WildInk,
    surface = WildPanel,
    surfaceVariant = WildPanel2,
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    onSurfaceVariant = WildMuted,
    outline = WildLine,
    error = WildRed
)

private val WildsportTypography = Typography(
    displayLarge = Typography().displayLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.6).sp
    ),
    displaySmall = Typography().displaySmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.8).sp
    ),
    headlineLarge = Typography().headlineLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = Typography().headlineMedium.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        letterSpacing = (-0.35).sp
    ),
    headlineSmall = Typography().headlineSmall.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold
    ),
    titleLarge = Typography().titleLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold
    ),
    labelLarge = Typography().labelLarge.copy(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.7.sp
    ),
    bodyLarge = Typography().bodyLarge.copy(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 24.sp
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = FontFamily.SansSerif,
        lineHeight = 21.sp
    )
)

private val WildsportShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun Objectif30Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = WildsportColors,
        typography = WildsportTypography,
        shapes = WildsportShapes,
        content = content
    )
}
