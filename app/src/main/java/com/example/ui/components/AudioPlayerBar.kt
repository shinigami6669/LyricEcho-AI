package com.example.ui.components

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
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AudioPlayerBar(
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isAutoScrollEnabled: Boolean,
    playbackSpeed: Float,
    targetLanguage: String,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleAutoScroll: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onOpenLanguageSelector: () -> Unit,
    onCycleFontSize: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("audio_player_bar")
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .border(
                1.dp,
                Brush.verticalGradient(listOf(ObsidianBorder, Color.Transparent)),
                RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ),
        color = ObsidianSurface.copy(alpha = 0.95f),
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            // Scrubber Slider
            val progress = if (durationMs > 0) {
                (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f)
            } else 0f

            Slider(
                value = progress,
                onValueChange = { frac ->
                    onSeek((frac * durationMs).toLong())
                },
                colors = SliderDefaults.colors(
                    thumbColor = ElectricCyan,
                    activeTrackColor = NeonViolet,
                    inactiveTrackColor = ObsidianBorder
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
                    .testTag("player_progress_slider")
            )

            // Time Labels & Utility Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatMs(currentPositionMs),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )

                // Language quick chip
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2536))
                        .clickable(onClick = onOpenLanguageSelector)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = "Language",
                        tint = ElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = targetLanguage,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Font size cycle button
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF1E2536))
                        .clickable(onClick = onCycleFontSize)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.FormatSize,
                        contentDescription = "Font size",
                        tint = NeonViolet,
                        modifier = Modifier.size(12.dp)
                    )
                }

                Text(
                    text = formatMs(durationMs),
                    color = TextMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Controls Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Auto Scroll Toggle Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isAutoScrollEnabled) NeonViolet.copy(alpha = 0.2f) else Color.Transparent
                        )
                        .border(
                            1.dp,
                            if (isAutoScrollEnabled) NeonViolet else ObsidianBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable(onClick = onToggleAutoScroll)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("auto_scroll_toggle")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Auto Scroll",
                            tint = if (isAutoScrollEnabled) NeonViolet else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAutoScrollEnabled) "Scroll ON" else "Scroll OFF",
                            color = if (isAutoScrollEnabled) TextPrimary else TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Playback Center Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Rewind 5s
                    IconButton(
                        onClick = { onSeek((currentPositionMs - 5000L).coerceAtLeast(0L)) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("rewind_5s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastRewind,
                            contentDescription = "Rewind 5s",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    // Play / Pause glowing pill
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(listOf(NeonViolet, ElectricCyan))
                            )
                            .clickable(onClick = onTogglePlayPause)
                            .testTag("play_pause_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    // Fast Forward 5s
                    IconButton(
                        onClick = { onSeek((currentPositionMs + 5000L).coerceAtMost(durationMs)) },
                        modifier = Modifier
                            .size(38.dp)
                            .testTag("forward_5s_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FastForward,
                            contentDescription = "Forward 5s",
                            tint = TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Speed Selector Pill (0.75x, 1.0x, 1.25x)
                val nextSpeed = when (playbackSpeed) {
                    0.75f -> 1.0f
                    1.0f -> 1.25f
                    1.25f -> 1.5f
                    else -> 0.75f
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E2536))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .clickable { onChangeSpeed(nextSpeed) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                        .testTag("playback_speed_button")
                ) {
                    Text(
                        text = "${playbackSpeed}x",
                        color = ElectricCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).toInt()
    val min = totalSec / 60
    val sec = totalSec % 60
    return "%02d:%02d".format(min, sec)
}
