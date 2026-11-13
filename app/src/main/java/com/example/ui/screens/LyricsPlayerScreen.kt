package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.LyricLine
import com.example.data.model.SongLyric
import com.example.ui.components.AudioPlayerBar
import com.example.ui.components.BilingualLyricCard
import com.example.ui.components.CacheBadge
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun LyricsPlayerScreen(
    song: SongLyric?,
    lines: List<LyricLine>,
    isFromCache: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    activeLineIndex: Int,
    isAutoScrollEnabled: Boolean,
    playbackSpeed: Float,
    fontSize: String,
    targetLanguage: String,
    supportedLanguages: List<String>,
    isLoading: Boolean,
    onBack: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSeekToLine: (LyricLine) -> Unit,
    onToggleAutoScroll: () -> Unit,
    onChangeSpeed: (Float) -> Unit,
    onCycleFontSize: () -> Unit,
    onRetranslate: (String) -> Unit,
    onToggleFavorite: (SongLyric) -> Unit,
    onPopOutFloating: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var showLanguagePicker by remember { mutableStateOf(false) }

    BackHandler {
        onBack()
    }

    // Auto-scroll effect: smoothly scroll to active line whenever index changes and auto-scroll is enabled
    LaunchedEffect(activeLineIndex, isAutoScrollEnabled) {
        if (isAutoScrollEnabled && activeLineIndex in lines.indices) {
            // Scroll so the active line is near the upper center
            val scrollTarget = (activeLineIndex - 1).coerceAtLeast(0)
            listState.animateScrollToItem(scrollTarget)
        }
    }

    if (showLanguagePicker) {
        LanguageSelectorDialog(
            currentLanguage = targetLanguage,
            supportedLanguages = supportedLanguages,
            onLanguageSelected = { newLang ->
                onRetranslate(newLang)
            },
            onDismiss = { showLanguagePicker = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(ObsidianBg)
    ) {
        // Subtle ambient radial glow behind lyrics
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            NeonViolet.copy(alpha = 0.09f),
                            ElectricCyan.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("btn_back_to_home")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = song?.title ?: "Loading...",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = song?.artist ?: "",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        CacheBadge(isFromCache = isFromCache)
                    }
                }

                // AI Re-translate Button
                IconButton(
                    onClick = { showLanguagePicker = true },
                    modifier = Modifier.testTag("btn_retranslate_dialog")
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = ElectricCyan,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "Re-translate with AI",
                            tint = ElectricCyan
                        )
                    }
                }

                // Favorite Toggle
                if (song != null) {
                    IconButton(
                        onClick = { onToggleFavorite(song) },
                        modifier = Modifier.testTag("btn_favorite_toggle")
                    ) {
                        Icon(
                            imageVector = if (song.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (song.isFavorite) SunsetRose else TextMuted
                        )
                    }
                }

                // Share Button
                IconButton(
                    onClick = {
                        if (song != null) {
                            val shareBody = """
                                🎵 ${song.title} - ${song.artist}
                                [Bilingual Lyrics: Original & ${song.language} Translation]
                                
                                ${lines.joinToString("\n\n") { "${it.original}\n${it.translated}" }}
                                
                                — Shared via LyricEcho AI
                            """.trimIndent()
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, shareBody)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share Bilingual Lyrics"))
                        }
                    },
                    modifier = Modifier.testTag("btn_share_lyrics")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = TextSecondary
                    )
                }

                // Floating Pop-up button (Musixmatch style)
                IconButton(
                    onClick = onPopOutFloating,
                    modifier = Modifier.testTag("btn_popout_floating")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Floating Lyrics Pop-up",
                        tint = ElectricCyan
                    )
                }
            }

            // Dual-line synchronized lyrics list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                if (lines.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = NeonViolet)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Fetching & translating with Gemini AI...",
                                    color = TextSecondary,
                                    fontSize = 14.sp
                                )
                            }
                        } else {
                            Text(
                                text = "No lyrics available for this song.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(top = 10.dp, bottom = 120.dp)
                    ) {
                        itemsIndexed(lines, key = { _, item -> item.id }) { index, line ->
                            BilingualLyricCard(
                                line = line,
                                isActive = (index == activeLineIndex),
                                fontSizeSetting = fontSize,
                                targetLanguage = song?.language ?: targetLanguage,
                                onLineClick = { onSeekToLine(line) }
                            )
                        }
                    }
                }
            }

            // Pinned Bottom Audio Player Bar
            AudioPlayerBar(
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                isAutoScrollEnabled = isAutoScrollEnabled,
                playbackSpeed = playbackSpeed,
                targetLanguage = song?.language ?: targetLanguage,
                onTogglePlayPause = onTogglePlayPause,
                onSeek = onSeek,
                onToggleAutoScroll = onToggleAutoScroll,
                onChangeSpeed = onChangeSpeed,
                onOpenLanguageSelector = { showLanguagePicker = true },
                onCycleFontSize = onCycleFontSize,
                modifier = Modifier.navigationBarsPadding()
            )
        }
    }
}
