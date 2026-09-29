package com.yanis.objectif30.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

val WildCyan = Color(0xFF22E7F4)
val WildBlue = Color(0xFF3385FF)
val WildViolet = Color(0xFF8C7CFF)
val WildGreen = Color(0xFF60E6A8)
val WildAmber = Color(0xFFFFC85C)
val WildRed = Color(0xFFFF7588)
val WildInk = Color(0xFF05080D)
val WildPanel = Color(0xFF0B1118)
val WildPanel2 = Color(0xFF101923)
val WildLine = Color(0xFF223443)
val WildMuted = Color(0xFF8FA6B2)

@Composable
fun WildBackdrop(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0f to Color(0xFF061017),
                    0.35f to Color(0xFF071018),
                    1f to WildInk
                )
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            WildBlue.copy(alpha = 0.14f),
                            WildCyan.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )
        content()
    }
}

@Composable
fun WildGlassCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = if (highlighted) Color(0xFF0D222A) else WildPanel.copy(alpha = 0.96f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(
            1.dp,
            if (highlighted) WildCyan.copy(alpha = 0.55f) else WildLine.copy(alpha = 0.9f)
        ),
        tonalElevation = if (highlighted) 3.dp else 0.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            content = content
        )
    }
}

@Composable
fun WildEyebrow(text: String) {
    Text(
        text = text.uppercase(),
        color = WildCyan,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black
    )
}

@Composable
fun WildSectionTitle(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    subtitle,
                    color = WildMuted,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun WildMetricTile(
    label: String,
    value: String,
    helper: String? = null,
    accent: Color = WildCyan,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = WildPanel2,
        border = BorderStroke(1.dp, WildLine.copy(alpha = 0.8f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(accent)
            )
            Text(
                value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Black
            )
            Text(label, color = WildMuted, style = MaterialTheme.typography.bodySmall)
            if (!helper.isNullOrBlank()) {
                Text(helper, color = accent, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun WildTag(
    text: String,
    accent: Color = WildCyan
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun WildPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = WildCyan,
            contentColor = Color(0xFF001317)
        )
    ) {
        Text(text, fontWeight = FontWeight.Black)
    }
}

@Composable
fun WildSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, WildLine)
    ) {
        Text(text, fontWeight = FontWeight.Bold)
    }
}
