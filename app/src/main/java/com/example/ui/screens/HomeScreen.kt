package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.provider.Settings
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.TextButton
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import com.example.data.remote.ApiEngineSettings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.platform.LocalContext
import com.example.service.MusicNotificationListener
import com.example.ui.components.PermissionPromptDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SongLyric
import com.example.ui.components.CacheBadge
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.components.ManualLyricInputDialog
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianSurfaceVariant
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    songs: List<SongLyric>,
    isLoading: Boolean,
    errorMessage: String?,
    targetLanguage: String,
    supportedLanguages: List<String>,
    onSelectSong: (SongLyric) -> Unit,
    onFetchWithAi: (title: String, artist: String) -> Unit,
    onLanguageChange: (String) -> Unit,
    onSaveManualLyric: (title: String, artist: String, language: String, original: String, translated: String) -> Unit,
    onNavigateToLibrary: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    onDismissError: () -> Unit,
    activeSong: SongLyric? = null,
    isFloatingPopupVisible: Boolean = false,
    onShowFloatingPopup: () -> Unit = {},
    apiEngineSettings: ApiEngineSettings = ApiEngineSettings(),
    isAppMasterEnabled: Boolean = true,
    onToggleMasterPower: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showManualInputDialog by remember { mutableStateOf(false) }
    var searchArtistInput by remember { mutableStateOf("") }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = targetLanguage,
            supportedLanguages = supportedLanguages,
            onLanguageSelected = onLanguageChange,
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showManualInputDialog) {
        ManualLyricInputDialog(
            currentLanguage = targetLanguage,
            onSaveManualLyric = onSaveManualLyric,
            onDismiss = { showManualInputDialog = false }
        )
    }

    val context = LocalContext.current
    var showPermissionDialog by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var hasNotifListener by remember { mutableStateOf(MusicNotificationListener.isNotificationListenerGranted(context)) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        hasOverlay = Settings.canDrawOverlays(context)
        hasNotifListener = MusicNotificationListener.isNotificationListenerGranted(context)
    }

    if (showPermissionDialog) {
        PermissionPromptDialog(
            onDismiss = {
                showPermissionDialog = false
                hasOverlay = Settings.canDrawOverlays(context)
                hasNotifListener = MusicNotificationListener.isNotificationListenerGranted(context)
            }
        )
    }

    val themeBg = androidx.compose.material3.MaterialTheme.colorScheme.background
    val themeSurface = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val themePrimary = androidx.compose.material3.MaterialTheme.colorScheme.primary
    val themeSecondary = androidx.compose.material3.MaterialTheme.colorScheme.secondary

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(themeBg)
            .statusBarsPadding()
    ) {
        // App Top Bar - Sleek, Responsive & Overflow-Proof
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App Logo & Title (Flexes to never push right buttons off)
            Row(
                modifier = Modifier.weight(1f, fill = false),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            Brush.linearGradient(listOf(themePrimary, themeSecondary))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "LyricEcho AI",
                        color = TextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Smart Bilingual Lyrics",
                        color = themeSecondary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Action controls (Master Power Pill + Action Icons + Prominent Settings)
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Master Power ON/OFF Toggle Pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (isAppMasterEnabled) Color(0xFF10B981).copy(alpha = 0.18f) else SunsetRose.copy(alpha = 0.18f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isAppMasterEnabled) Color(0xFF10B981) else SunsetRose),
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onToggleMasterPower)
                        .testTag("home_master_power_toggle")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (isAppMasterEnabled) Color(0xFF10B981) else SunsetRose)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isAppMasterEnabled) "روشن" else "خاموش",
                            color = if (isAppMasterEnabled) Color(0xFF34D399) else SunsetRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Floating Popup Shortcut
                IconButton(
                    onClick = onShowFloatingPopup,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("home_btn_floating_popup")
                ) {
                    Icon(
                        imageVector = Icons.Default.PictureInPictureAlt,
                        contentDescription = "Show Floating Lyrics Pop-up",
                        tint = if (isFloatingPopupVisible) ElectricCyan else TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Library Shortcut
                IconButton(
                    onClick = onNavigateToLibrary,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("nav_library_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.LibraryMusic,
                        contentDescription = "Library",
                        tint = TextSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Diagnostics Shortcut
                IconButton(
                    onClick = onNavigateToDiagnostics,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("nav_diagnostics_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.BugReport,
                        contentDescription = "سیستم لاگ و عیب‌یابی",
                        tint = themePrimary,
                        modifier = Modifier.size(17.dp)
                    )
                }

                // Prominent Settings Gear Button (ALWAYS VISIBLE!)
                IconButton(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(themeSurface)
                        .border(1.dp, themeSecondary.copy(alpha = 0.7f), RoundedCornerShape(8.dp))
                        .testTag("nav_settings_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = themeSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Master Power OFF Warning Banner
        AnimatedVisibility(visible = !isAppMasterEnabled) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(10.dp),
                color = SunsetRose.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(1.dp, SunsetRose.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = null,
                        tint = SunsetRose,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "برنامه خاموش است؛ رهگیری آهنگ و پاپ‌آپ غیرفعال می‌باشند.",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        style = androidx.compose.ui.text.TextStyle(
                            textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onToggleMasterPower) {
                        Text("روشن کردن", color = SunsetRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Active AI Engine & Translation Model Indicator (Full model name, never truncated to 'na...')
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(
                    1.dp,
                    Brush.horizontalGradient(
                        listOf(NeonViolet.copy(alpha = 0.5f), ElectricCyan.copy(alpha = 0.5f))
                    ),
                    RoundedCornerShape(12.dp)
                )
                .clickable { onNavigateToSettings() }
                .testTag("home_active_ai_badge"),
            color = Color(0xCC0E1322)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF00E676))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "AI:",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(NeonViolet.copy(alpha = 0.25f))
                            .border(0.5.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 5.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = when (apiEngineSettings.providerType) {
                                com.example.data.remote.ApiProviderType.CUSTOM_OPENAI -> "OpenAI"
                                com.example.data.remote.ApiProviderType.GOOGLE_GEMINI -> "Gemini"
                                com.example.data.remote.ApiProviderType.OPENROUTER -> "OpenRouter"
                                com.example.data.remote.ApiProviderType.DEEPSEEK -> "DeepSeek"
                                com.example.data.remote.ApiProviderType.GROQ -> "Groq"
                                com.example.data.remote.ApiProviderType.OPENAI_OFFICIAL -> "OpenAI"
                                else -> "AI"
                            },
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ElectricCyan.copy(alpha = 0.18f))
                            .border(0.5.dp, ElectricCyan.copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = apiEngineSettings.selectedModel,
                            color = ElectricCyan,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF141D2D))
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "تنظیمات",
                        color = ElectricCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "تنظیمات API",
                        tint = ElectricCyan,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }

        // Quick Setup Guide Banner when API Key is not yet configured
        AnimatedVisibility(visible = apiEngineSettings.apiKey.isBlank()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .clickable { onNavigateToSettings() }
                    .testTag("home_api_key_guide_banner"),
                color = Color(0xFF0B1526)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "🔑 دریافت کلید رایگان هوش مصنوعی (API Key)",
                            color = ElectricCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "برای فعال‌سازی ترجمه خودکار، کلید رایگان خود را از aistudio.google.com/api-keys دریافت و در تنظیمات وارد کنید.",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            style = androidx.compose.ui.text.TextStyle(
                                textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://aistudio.google.com/api-keys")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                onNavigateToSettings()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("دریافت کلید", color = Color.Black, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Spotify & Overlay Permission Warning Banner
        if (!hasNotifListener || !hasOverlay) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, SunsetRose.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .clickable { showPermissionDialog = true },
                color = Color(0xFF1E1118)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = SunsetRose,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تشخیص آهنگ اسپاتیفای نیاز به دسترسی دارد",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (!hasNotifListener) "دسترسی نوتیفیکیشن را فعال کنید تا موزیک در حال پخش شناسایی شود" else "دسترسی نمایش روی سایر برنامه‌ها فعال نیست",
                            color = SunsetRose,
                            fontSize = 10.sp
                        )
                    }
                    Button(
                        onClick = { showPermissionDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetRose),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("فعال‌سازی", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Search Section Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            color = themeSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, ObsidianBorder)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Song Title Input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search song title or lyrics...", color = TextMuted, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = NeonViolet
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear",
                                    tint = TextMuted
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedBorderColor = NeonViolet,
                        unfocusedBorderColor = ObsidianBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("search_song_input")
                )

                // Optional Artist for precision AI search
                AnimatedVisibility(visible = searchQuery.isNotBlank()) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchArtistInput,
                            onValueChange = { searchArtistInput = it },
                            placeholder = { Text("Artist name (optional for AI search)", color = TextMuted, fontSize = 13.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary,
                                focusedBorderColor = ElectricCyan,
                                unfocusedBorderColor = ObsidianBorder
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("search_artist_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Fetch with AI Button
                        Button(
                            onClick = {
                                onFetchWithAi(searchQuery.trim(), searchArtistInput.trim())
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = NeonViolet
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("btn_fetch_ai_lyrics")
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Translating with Gemini AI...", fontSize = 13.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = ElectricCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Fetch & Translate \"$searchQuery\" ($targetLanguage)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Error message banner
        if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF451A1A))
                    .border(1.dp, SunsetRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = errorMessage,
                        color = Color(0xFFFCA5A5),
                        fontSize = 12.sp,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = onDismissError, modifier = Modifier.size(24.dp)) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Dismiss error",
                            tint = Color(0xFFFCA5A5),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Action Chips Row (Language selector & Add custom song)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Target Language Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(ObsidianSurfaceVariant)
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(14.dp))
                    .clickable { showLanguageDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("target_language_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Translate,
                        contentDescription = null,
                        tint = ElectricCyan,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Target: $targetLanguage",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Add Custom Song Pill
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonViolet.copy(alpha = 0.15f))
                    .border(1.dp, NeonViolet.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .clickable { showManualInputDialog = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("add_custom_song_chip")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = NeonViolet,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Custom Song",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Section Title: Offline Cached Songs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (searchQuery.isBlank()) "Recent Offline Library" else "Search Results (${songs.size})",
                color = TextSecondary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.3.sp
            )
            Text(
                text = "${songs.size} cached",
                color = ElectricCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // List of Songs
        if (songs.isEmpty() && !isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = null,
                        tint = ElectricCyan.copy(alpha = 0.6f),
                        modifier = Modifier.size(52.dp)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "کتابخانه شما خالی است (Library Empty)" else "No songs found for \"$searchQuery\"",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (searchQuery.isBlank()) 
                            "هر آهنگی را در اسپاتیفای پلی کنید تا پاپ‌آپ خودکار لیریک ظاهر شود، یا در بالا نام آهنگ را جستجو کنید."
                            else "Tap 'Fetch & Translate' above to generate with Gemini AI!",
                        color = TextMuted,
                        fontSize = 12.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(songs, key = { it.id }) { song ->
                    SongListItem(
                        song = song,
                        onClick = { onSelectSong(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SongListItem(
    song: SongLyric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .testTag("song_item_${song.id}")
            .clip(RoundedCornerShape(16.dp))
            .background(ObsidianSurface)
            .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Song disc avatar
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF1E2536))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = null,
                    tint = NeonViolet,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Title, Artist and Language
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = song.title,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (song.isFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = SunsetRose,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${song.artist} • ${song.language}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(6.dp))
                CacheBadge(isFromCache = true)
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Play Pill
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(NeonViolet.copy(alpha = 0.2f))
                    .border(1.dp, NeonViolet, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play lyrics",
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
