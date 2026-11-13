package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricLine
import com.example.data.model.SongLyric
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTranslation
import kotlin.math.roundToInt

@Composable
fun FloatingLyricsOverlay(
    song: SongLyric?,
    lines: List<LyricLine>,
    activeLineIndex: Int,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    targetLanguage: String,
    isCollapsed: Boolean,
    onToggleCollapse: () -> Unit,
    onExpandToFullScreen: () -> Unit,
    onClose: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekRelative: (Long) -> Unit,
    syncOffsetMs: Long = 0L,
    onAdjustSyncOffset: (Long) -> Unit = {},
    onResetSyncOffset: () -> Unit = {},
    onRefresh: () -> Unit = {},
    popupScale: String = "Medium",
    tapToCollapseEnabled: Boolean = true,
    themeMode: com.example.ui.theme.AppThemeMode = com.example.ui.theme.AppThemeMode.NEON_OBSIDIAN,
    modifier: Modifier = Modifier
) {
    if (song == null) return

    // Offset in pixels for dragging the floating window
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(80f) }

    val activeLine = lines.getOrNull(activeLineIndex)
    val nextLine = lines.getOrNull(activeLineIndex + 1)
    val isRtl = targetLanguage.equals("Persian", true) ||
            targetLanguage.equals("Farsi", true) ||
            targetLanguage.equals("Arabic", true)

    val (cardMinWidth, cardMaxWidth, lyricFontSize, transFontSize) = when (popupScale) {
        "Small" -> listOf(240.dp, 280.dp, 12.sp, 11.sp)
        "Large" -> listOf(330.dp, 390.dp, 16.sp, 15.sp)
        "Extra Large", "XL" -> listOf(370.dp, 440.dp, 18.sp, 17.sp)
        else -> listOf(280.dp, 340.dp, 14.sp, 13.sp)
    }

    val primaryColor = themeMode.primaryColor
    val secondaryColor = themeMode.secondaryColor

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
            .testTag("floating_lyrics_overlay")
    ) {
        if (isCollapsed) {
            // Minimized Floating Music Bubble showing App Logo
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .shadow(12.dp, CircleShape)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(listOf(primaryColor, Color(0xFF10141E)))
                    )
                    .border(2.dp, secondaryColor, CircleShape)
                    .clickable(onClick = onToggleCollapse)
                    .testTag("floating_bubble_expand_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = "Expand Floating Lyrics",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        } else {
            // Expanded Floating Glassmorphic Lyric Card
            Surface(
                modifier = Modifier
                    .widthIn(min = cardMinWidth as androidx.compose.ui.unit.Dp, max = cardMaxWidth as androidx.compose.ui.unit.Dp)
                    .shadow(18.dp, RoundedCornerShape(20.dp))
                    .clip(RoundedCornerShape(20.dp))
                    .border(
                        1.5.dp,
                        Brush.horizontalGradient(listOf(primaryColor, secondaryColor)),
                        RoundedCornerShape(20.dp)
                    ),
                color = ObsidianSurface.copy(alpha = 0.94f),
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Header Bar (Drag Handle + Actions)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isPlaying) ElectricCyan else TextMuted)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = song.title,
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${song.artist} • $targetLanguage",
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Action Buttons: Collapse, Fullscreen, Close
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onRefresh,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Re-sync lyrics",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onToggleCollapse,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Remove,
                                    contentDescription = "Minimize bubble",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            IconButton(
                                onClick = onExpandToFullScreen,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Open full player",
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(
                                onClick = onClose,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close floating lyrics",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Time Display Row (زمان رد شده | زمان کل | زمان باقی‌مانده)
                    val effectiveDuration = if (durationMs > 0) durationMs else 180000L
                    val remainingMs = (effectiveDuration - currentPositionMs).coerceAtLeast(0L)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 1. زمان رد شده (Elapsed Time)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(ElectricCyan)
                            )
                            Text(
                                text = formatTime(currentPositionMs),
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "رد شده",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                        }

                        // 2. زمان کل آهنگ (Total Song Duration)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF192030))
                                .padding(horizontal = 6.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = "کل: ${formatTime(effectiveDuration)}",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        // 3. زمان باقی‌مانده (Remaining Time)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "باقی:",
                                color = TextMuted,
                                fontSize = 9.sp
                            )
                            Text(
                                text = "-${formatTime(remainingMs)}",
                                color = SunsetRose,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Progress Bar
                    val progress = if (effectiveDuration > 0) {
                        (currentPositionMs.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f)
                    } else 0f
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = NeonViolet,
                        trackColor = ObsidianBorder
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Active Lyric Lines
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1B38).copy(alpha = 0.7f))
                            .clickable(enabled = tapToCollapseEnabled) {
                                onToggleCollapse()
                            }
                            .padding(12.dp)
                    ) {
                        Column {
                            // Original line
                            Text(
                                text = activeLine?.original ?: "♫ Musical Intro / Interlude ♫",
                                color = TextPrimary,
                                fontSize = lyricFontSize as androidx.compose.ui.unit.TextUnit,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Translated line
                            val hasTrans = !activeLine?.translated.isNullOrBlank() && activeLine?.translated?.trim() != activeLine?.original?.trim()
                            if (hasTrans) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = activeLine!!.translated,
                                    color = secondaryColor,
                                    fontSize = transFontSize as androidx.compose.ui.unit.TextUnit,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = if (isRtl) TextAlign.End else TextAlign.Start,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = if (isRtl) TextDirection.Rtl else TextDirection.Ltr
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            } else if (activeLine != null) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "⏳ در حال ترجمه با هوش مصنوعی...",
                                    color = SunsetRose.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Next lyric preview
                            if (nextLine != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Next: ${nextLine.original}",
                                    color = TextMuted,
                                    fontSize = 10.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sync Offset Precision Controls
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0C101A))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (syncOffsetMs == 0L) "سینک: 0.0s" else "سینک: ${if (syncOffsetMs > 0) "+" else ""}${syncOffsetMs / 1000.0}s",
                            color = if (syncOffsetMs == 0L) TextSecondary else NeonViolet,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2536))
                                    .clickable { onAdjustSyncOffset(-500L) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("-0.5s", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF1E2536))
                                    .clickable { onAdjustSyncOffset(500L) }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("+0.5s", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }

                            if (syncOffsetMs != 0L) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(SunsetRose.copy(alpha = 0.2f))
                                        .clickable { onResetSyncOffset() }
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text("Reset", color = SunsetRose, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mini Playback Controls
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatTime(currentPositionMs),
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { onSeekRelative(-5000L) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastRewind,
                                    contentDescription = "-5s",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(listOf(NeonViolet, ElectricCyan))
                                    )
                                    .clickable(onClick = onTogglePlayPause),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            IconButton(
                                onClick = { onSeekRelative(5000L) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FastForward,
                                    contentDescription = "+5s",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "-${formatTime(remainingMs)}",
                            color = SunsetRose,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val sec = (ms / 1000).toInt()
    return "%02d:%02d".format(sec / 60, sec % 60)
}
