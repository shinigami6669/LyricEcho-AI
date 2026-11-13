package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricLine
import com.example.ui.theme.ActiveLineGradientEnd
import com.example.ui.theme.ActiveLineGradientStart
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTranslation

@Composable
fun BilingualLyricCard(
    line: LyricLine,
    isActive: Boolean,
    fontSizeSetting: String,
    targetLanguage: String,
    onLineClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (originalFontSize, translatedFontSize) = when (fontSizeSetting) {
        "Small" -> Pair(15.sp, 13.sp)
        "Large" -> Pair(22.sp, 19.sp)
        "Extra Large" -> Pair(26.sp, 22.sp)
        else -> Pair(18.sp, 15.sp) // Medium default
    }

    val isRtlLanguage = targetLanguage.equals("Persian", ignoreCase = true) ||
            targetLanguage.equals("Farsi", ignoreCase = true) ||
            targetLanguage.equals("Arabic", ignoreCase = true)

    val animatedBg by animateColorAsState(
        targetValue = if (isActive) Color(0xFF22173B) else Color(0xFF131722).copy(alpha = 0.6f),
        animationSpec = tween(durationMillis = 250),
        label = "bgAnim"
    )

    val borderModifier = if (isActive) {
        Modifier.border(
            width = 1.5.dp,
            brush = Brush.horizontalGradient(
                colors = listOf(NeonViolet, ElectricCyan)
            ),
            shape = RoundedCornerShape(16.dp)
        )
    } else {
        Modifier.border(
            width = 1.dp,
            color = ObsidianBorder.copy(alpha = 0.5f),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("lyric_card_${line.id}")
            .clip(RoundedCornerShape(16.dp))
            .then(borderModifier)
            .background(animatedBg)
            .clickable(onClick = onLineClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Active indicator pill
            if (isActive) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(NeonViolet.copy(alpha = 0.3f), CircleShape)
                        .border(1.dp, NeonViolet, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Active line",
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Original Song Lyric
                Text(
                    text = line.original,
                    color = if (isActive) TextPrimary else TextSecondary,
                    fontSize = originalFontSize,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                    lineHeight = (originalFontSize.value * 1.35).sp
                )

                // Translated Lyric (Bilingual stacked underneath)
                if (line.translated.isNotBlank()) {
                    Text(
                        text = line.translated,
                        color = if (isActive) ElectricCyan else TextTranslation.copy(alpha = 0.75f),
                        fontSize = translatedFontSize,
                        fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = if (isRtlLanguage) TextAlign.End else TextAlign.Start,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = if (isRtlLanguage) TextDirection.Rtl else TextDirection.Ltr
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        lineHeight = (translatedFontSize.value * 1.4).sp
                    )
                }
            }

            // Timestamp on the right
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = line.formatTimestamp(),
                color = if (isActive) NeonViolet else TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.Top)
            )
        }
    }
}
