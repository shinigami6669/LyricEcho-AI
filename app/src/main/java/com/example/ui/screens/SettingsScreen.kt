package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.TouchApp
import com.example.ui.theme.AppThemeMode
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.remote.ApiEngineSettings
import com.example.data.remote.ApiProfile
import com.example.data.remote.ApiProviderType
import com.example.data.remote.EndpointPingResult
import com.example.ui.components.ApiHubCard
import com.example.ui.components.LanguageSelectorDialog
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    currentTargetLanguage: String,
    supportedLanguages: List<String>,
    currentApiKey: String,
    currentFontSize: String,
    isAutoScrollEnabled: Boolean,
    isAutoPopupEnabled: Boolean,
    isSpotifyDetectEnabled: Boolean,
    totalCachedCount: Int,
    apiEngineSettings: ApiEngineSettings = ApiEngineSettings(),
    totalRequests: Int = 0,
    successRequests: Int = 0,
    failedRequests: Int = 0,
    isDetectingModels: Boolean = false,
    detectModelsResult: String? = null,
    pingResult: EndpointPingResult? = null,
    isPinging: Boolean = false,
    isTestingApi: Boolean = false,
    apiTestResult: String? = null,
    rawTerminalOutput: String = "",
    savedProfiles: List<ApiProfile> = emptyList(),
    activeProfileId: String = "",
    onLanguageChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onFontSizeChange: (String) -> Unit,
    onToggleAutoScroll: () -> Unit,
    onToggleAutoPopup: () -> Unit,
    onToggleSpotifyDetect: () -> Unit,
    onSimulateSpotify: (track: String, artist: String) -> Unit,
    onProviderTypeChange: (ApiProviderType) -> Unit = {},
    onBaseUrlChange: (String) -> Unit = {},
    onModelChange: (String) -> Unit = {},
    onMaxRetriesChange: (Int) -> Unit = {},
    onTimeoutSecondsChange: (Int) -> Unit = {},
    onRpmLimitChange: (Int) -> Unit = {},
    onAutoFallbackChange: (Boolean) -> Unit = {},
    onBackupKeyChange: (String) -> Unit = {},
    onDetectModels: () -> Unit = {},
    onPingEndpoint: () -> Unit = {},
    onResetStats: () -> Unit = {},
    onTestApiKey: (String) -> Unit = {},
    onCancelTestApi: () -> Unit = {},
    onClearTerminal: () -> Unit = {},
    onSaveProfile: (String) -> Unit = {},
    onDeleteProfile: (String) -> Unit = {},
    onActivateProfile: (ApiProfile) -> Unit = {},
    onTestFloatingPopup: () -> Unit = {},
    onTestDatabase: () -> Unit = {},
    onClearCache: () -> Unit,
    onNavigateToDiagnostics: () -> Unit = {},
    isAppMasterEnabled: Boolean = true,
    onToggleMasterPower: () -> Unit = {},
    currentThemeMode: AppThemeMode = AppThemeMode.NEON_OBSIDIAN,
    onThemeChange: (AppThemeMode) -> Unit = {},
    popupScale: String = "Medium",
    onPopupScaleChange: (String) -> Unit = {},
    popupTapToCollapseEnabled: Boolean = true,
    onTogglePopupTapToCollapse: () -> Unit = {},
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isListenerConnected by com.example.service.MusicNotificationListener.isListenerConnected.collectAsStateWithLifecycle()
    var isListenerGranted by remember { mutableStateOf(com.example.service.MusicNotificationListener.isNotificationListenerGranted(context)) }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        isListenerGranted = com.example.service.MusicNotificationListener.isNotificationListenerGranted(context)
    }

    var showLanguageDialog by remember { mutableStateOf(false) }
    var showLanguagePreview by remember { mutableStateOf(false) }
    var showLyricsDisplayPreview by remember { mutableStateOf(false) }
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Accordion State: By default, API Hub is open for convenience, others cleanly collapsed
    var expandedSections by remember { mutableStateOf(setOf("api")) }

    fun toggleSection(sectionId: String) {
        expandedSections = if (expandedSections.contains(sectionId)) {
            expandedSections - sectionId
        } else {
            expandedSections + sectionId
        }
    }

    BackHandler {
        onBack()
    }

    if (showLanguageDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentTargetLanguage,
            supportedLanguages = supportedLanguages,
            onLanguageSelected = onLanguageChange,
            onDismiss = { showLanguageDialog = false }
        )
    }

    if (showClearConfirmDialog) {
        BasicAlertDialog(onDismissRequest = { showClearConfirmDialog = false }) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(20.dp)),
                color = ObsidianSurface
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "بازنشانی حافظه و دیتابیس؟",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "با این کار تمام آهنگ‌های ذخیره‌شده محلی پاک شده و کتابخانه پیش‌فرض بازیابی می‌شود.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showClearConfirmDialog = false }) {
                            Text("انصراف", color = TextMuted)
                        }
                        TextButton(
                            onClick = {
                                onClearCache()
                                showClearConfirmDialog = false
                            }
                        ) {
                            Text("پاک‌سازی", color = SunsetRose, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(androidx.compose.material3.MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.testTag("settings_btn_back")) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = "تنظیمات و ترجیحات (Settings)",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ========================================================
            // MASTER POWER CARD: کلید خاموش/روشن کلی سیستم
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        1.5.dp,
                        if (isAppMasterEnabled) Color(0xFF10B981) else SunsetRose,
                        RoundedCornerShape(16.dp)
                    ),
                color = if (isAppMasterEnabled) Color(0xFF0D1B15) else Color(0xFF1F1117)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isAppMasterEnabled) Color(0xFF10B981).copy(alpha = 0.2f)
                                    else SunsetRose.copy(alpha = 0.2f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = null,
                                tint = if (isAppMasterEnabled) Color(0xFF10B981) else SunsetRose,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "کلید خاموش و روشن کلی برنامه (Master Power)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isAppMasterEnabled) "برنامه فعال است (رهگیری و پاپ‌آپ آماده)" else "برنامه خاموش است (بدون مصرف پس‌زمینه)",
                                color = if (isAppMasterEnabled) Color(0xFF34D399) else SunsetRose,
                                fontSize = 11.sp
                            )
                        }
                    }
                    Switch(
                        checked = isAppMasterEnabled,
                        onCheckedChange = { onToggleMasterPower() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color(0xFF10B981),
                            checkedTrackColor = Color(0xFF064E3B),
                            uncheckedThumbColor = SunsetRose,
                            uncheckedTrackColor = Color(0xFF4C0519)
                        ),
                        modifier = Modifier.testTag("switch_master_power")
                    )
                }
            }

            // ========================================================
            // SECTION: ظاهر و تم‌های برنامه (Theme Customization)
            // ========================================================
            SettingsAccordionCard(
                title = "طراحی و تم ظاهری برنامه",
                subtitle = currentThemeMode.displayName,
                icon = Icons.Default.Palette,
                iconTint = currentThemeMode.primaryColor,
                isExpanded = expandedSections.contains("theme"),
                onToggle = { toggleSection("theme") },
                testTag = "settings_accordion_theme"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "یکی از ۵ تم نئونی و مینیمال زیر را برای فضای برنامه و حاشیه پاپ‌آپ شناور انتخاب کنید:",
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                    AppThemeMode.entries.forEach { mode ->
                        val isSelected = mode == currentThemeMode
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(
                                    width = if (isSelected) 2.dp else 1.dp,
                                    color = if (isSelected) mode.primaryColor else ObsidianBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onThemeChange(mode) },
                            color = if (isSelected) mode.surfaceColor else Color(0xFF10141E)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    // Theme color palette preview
                                    Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(mode.primaryColor)
                                                .border(1.5.dp, Color(0xFF0D111A), CircleShape)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(mode.secondaryColor)
                                                .border(1.5.dp, Color(0xFF0D111A), CircleShape)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = mode.displayName,
                                        color = if (isSelected) Color.White else TextSecondary,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(mode.primaryColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ========================================================
            // SECTION 1: AI API HUB & PROFILE MANAGEMENT (آکاردئونی)
            // ========================================================
            SettingsAccordionCard(
                title = "اتصال و مدیریت API هوش مصنوعی",
                subtitle = "${apiEngineSettings.providerType.displayName} | ${apiEngineSettings.selectedModel}",
                icon = Icons.Default.Key,
                iconTint = NeonViolet,
                isExpanded = expandedSections.contains("api"),
                onToggle = { toggleSection("api") },
                testTag = "settings_accordion_api"
            ) {
                ApiHubCard(
                    settings = apiEngineSettings,
                    totalRequests = totalRequests,
                    successRequests = successRequests,
                    failedRequests = failedRequests,
                    isDetectingModels = isDetectingModels,
                    detectModelsResult = detectModelsResult,
                    pingResult = pingResult,
                    isPinging = isPinging,
                    isTestingApi = isTestingApi,
                    apiTestResult = apiTestResult,
                    rawTerminalOutput = rawTerminalOutput,
                    savedProfiles = savedProfiles,
                    activeProfileId = activeProfileId,
                    onProviderTypeChange = onProviderTypeChange,
                    onBaseUrlChange = onBaseUrlChange,
                    onApiKeyChange = onApiKeyChange,
                    onModelChange = onModelChange,
                    onMaxRetriesChange = onMaxRetriesChange,
                    onTimeoutSecondsChange = onTimeoutSecondsChange,
                    onRpmLimitChange = onRpmLimitChange,
                    onAutoFallbackChange = onAutoFallbackChange,
                    onBackupKeyChange = onBackupKeyChange,
                    onDetectModels = onDetectModels,
                    onPingEndpoint = onPingEndpoint,
                    onTestApi = onTestApiKey,
                    onCancelTestApi = onCancelTestApi,
                    onClearTerminal = onClearTerminal,
                    onResetStats = onResetStats,
                    onSaveProfile = onSaveProfile,
                    onDeleteProfile = onDeleteProfile,
                    onActivateProfile = onActivateProfile
                )
            }

            // ========================================================
            // SECTION 2: FLOATING POP-UP & SPOTIFY LIVE SYNC (آکاردئونی)
            // ========================================================
            val isPopupOpen = expandedSections.contains("popup")
            SettingsAccordionCard(
                title = "پاپ‌آپ شناور و هماهنگی اسپاتیفای",
                subtitle = if (isAutoPopupEnabled) "پاپ‌آپ خودکار فعال است" else "پاپ‌آپ خودکار خاموش",
                icon = Icons.Default.PictureInPictureAlt,
                iconTint = ElectricCyan,
                isExpanded = isPopupOpen,
                onToggle = { toggleSection("popup") },
                testTag = "settings_floating_lyrics_card"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Auto-show switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "باز شدن خودکار پاپ‌آپ با شروع آهنگ",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "هنگام شروع پخش موزیک، ویجت شناور دو زبانه Musixmatch باز می‌شود",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAutoPopupEnabled,
                            onCheckedChange = { onToggleAutoPopup() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("switch_auto_popup")
                        )
                    }

                    // Spotify auto-detect switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "رهگیری زنده اسپاتیفای (Spotify Detect)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "تشخیص فوری تغییر آهنگ در اسپاتیفای و ترجمه بلادرنگ",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isSpotifyDetectEnabled,
                            onCheckedChange = { onToggleSpotifyDetect() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF1DB954),
                                checkedTrackColor = Color(0xFF14532D)
                            ),
                            modifier = Modifier.testTag("switch_spotify_detect")
                        )
                    }

                    // Pop-up Size Scaling
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF111726))
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "اندازه پنجره شناور (Popup Size)",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = when (popupScale) {
                                    "Small" -> "کوچک"
                                    "Large" -> "بزرگ"
                                    "Extra Large", "XL" -> "خیلی بزرگ"
                                    else -> "معمولی"
                                },
                                color = ElectricCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "Small" to "کوچک",
                                "Medium" to "معمولی",
                                "Large" to "بزرگ",
                                "XL" to "بسیار بزرگ"
                            ).forEach { (scaleKey, label) ->
                                val isCur = popupScale.equals(scaleKey, ignoreCase = true)
                                Surface(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .border(
                                            1.dp,
                                            if (isCur) ElectricCyan else ObsidianBorder,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { onPopupScaleChange(scaleKey) },
                                    color = if (isCur) ElectricCyan.copy(alpha = 0.2f) else Color(0xFF172033)
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isCur) Color.White else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isCur) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Tap Center to Collapse
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "کوچک‌سازی سریع با لمس مرکز پاپ‌آپ (آرم اپ)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "با زدن انگشت وسط کارت لیریک، پنجره سریعاً به حباب گرد با آیکون موزیک کوچک می‌شود",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = popupTapToCollapseEnabled,
                            onCheckedChange = { onTogglePopupTapToCollapse() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = NeonViolet
                            ),
                            modifier = Modifier.testTag("switch_tap_to_collapse")
                        )
                    }

                    // 360-degree Drag Tip
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0F1A24)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.OpenWith, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "حرکت آزادانه از هر طرف (Full-Surface Drag): می‌توانید پاپ‌آپ را از هر لبه، کناره، حاشیه یا پس‌زمینه لمس کرده و به هر نقطه از صفحه بکشید.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    // Notification Listener Hook Card & Rebind
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                1.dp,
                                if (isListenerConnected) Color(0xFF10B981) else if (isListenerGranted) ElectricCyan else SunsetRose,
                                RoundedCornerShape(12.dp)
                            ),
                        color = Color(0xFF101420)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(8.dp)
                                                .clip(CircleShape)
                                                .background(if (isListenerConnected) Color(0xFF10B981) else SunsetRose)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "وضعیت اتصال شنونده اعلان‌ها (Notification Hook)",
                                            color = TextPrimary,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = when {
                                            isListenerConnected -> "متصل و فعال در پس‌زمینه (آماده دریافت خودکار)"
                                            isListenerGranted -> "مجوز فعال است اما سیستم قطع کرده (آماده ری‌کانکت)"
                                            else -> "دسترسی نوتیفیکیشن در تنظیمات داده نشده است"
                                        },
                                        color = if (isListenerConnected) Color(0xFF34D399) else SunsetRose,
                                        fontSize = 10.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                if (isListenerGranted) {
                                    Button(
                                        onClick = {
                                            com.example.service.MusicNotificationListener.rebindService(context, forceToggle = true)
                                            android.widget.Toast.makeText(context, "سرویس شنونده اعلان‌ها با موفقیت ری‌کانکت گردید", android.widget.Toast.LENGTH_SHORT).show()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp).testTag("btn_rebind_listener")
                                    ) {
                                        Icon(imageVector = Icons.Default.Sync, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ری‌کانکت خودکار سرویس", color = ElectricCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            com.example.service.MusicNotificationListener.openNotificationListenerSettings(context)
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SunsetRose.copy(alpha = 0.2f)),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, SunsetRose),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f).height(36.dp).testTag("btn_request_notif_permission")
                                    ) {
                                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, tint = SunsetRose, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("فعال‌سازی دسترسی نوتیفیکیشن", color = SunsetRose, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // System overlay permission helper button
                    Button(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
                                context.startActivity(intent)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2536)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_request_overlay_permission")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Layers,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "اعطای دسترسی نمایش روی سایر برنامه‌ها (Overlay)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // ========================================================
            // SECTION 3: TARGET LANGUAGE & TRANSLATION (آکاردئونی)
            // ========================================================
            SettingsAccordionCard(
                title = "زبان پیش‌فرض ترجمه لیریک",
                subtitle = "زبان فعلی: $currentTargetLanguage",
                icon = Icons.Default.Language,
                iconTint = ElectricCyan,
                isExpanded = expandedSections.contains("language"),
                onToggle = { toggleSection("language") },
                testTag = "settings_target_language_card"
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLanguageDialog = true },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "زبان انتخاب‌شده:",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Text(
                                text = currentTargetLanguage,
                                color = NeonViolet,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "تغییر زبان",
                            color = ElectricCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showLanguagePreview = !showLanguagePreview },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2536)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_language_translation")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = NeonViolet,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showLanguagePreview) "بستن پیش‌نمایش" else "تست پیش‌نمایش ترجمه ($currentTargetLanguage)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showLanguagePreview) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF161A26))
                                .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Original: I used to rule the world, seas would rise when I gave the word",
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (currentTargetLanguage.equals("Persian", true) || currentTargetLanguage.equals("Farsi", true))
                                        "ترجمه فارسی: زمانی بر جهان حکومت می‌کردم، با کلام من دریاها طغیان می‌کردند"
                                    else
                                        "Translation ($currentTargetLanguage): Sample translated line for $currentTargetLanguage...",
                                    color = ElectricCyan,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // SECTION 4: LYRICS APPEARANCE & AUTO-SCROLL (آکاردئونی)
            // ========================================================
            SettingsAccordionCard(
                title = "اندازه فونت و اسکرول کارائوکه",
                subtitle = "اندازه: $currentFontSize | ${if (isAutoScrollEnabled) "اسکرول خودکار فعال" else "اسکرول دستی"}",
                icon = Icons.Default.FormatSize,
                iconTint = NeonViolet,
                isExpanded = expandedSections.contains("font"),
                onToggle = { toggleSection("font") }
            ) {
                Column {
                    Text(
                        text = "انتخاب اندازه متن:",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    val fontOptions = listOf("Small", "Medium", "Large", "Extra Large")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        fontOptions.forEach { size ->
                            val isSelected = size.equals(currentFontSize, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) NeonViolet else Color(0xFF1E2536))
                                    .clickable { onFontSizeChange(size) }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (size == "Extra Large") "XL" else size,
                                    color = if (isSelected) Color.White else TextSecondary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "اسکرول خودکار کارائوکه (Auto-Scroll)",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "متن فعال همیشه در مرکز دید باقی می‌ماند",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        Switch(
                            checked = isAutoScrollEnabled,
                            onCheckedChange = { onToggleAutoScroll() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricCyan,
                                checkedTrackColor = NeonViolet
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { showLyricsDisplayPreview = !showLyricsDisplayPreview },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2536)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_font_autoscroll")
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showLyricsDisplayPreview) "بستن پیش‌نمایش" else "پیش‌نمایش اندازه فونت ($currentFontSize)",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (showLyricsDisplayPreview) {
                        val testFontSizeSp = when (currentFontSize.lowercase()) {
                            "small" -> 14.sp
                            "large" -> 20.sp
                            "extra large" -> 24.sp
                            else -> 17.sp
                        }
                        val testTranslationSizeSp = when (currentFontSize.lowercase()) {
                            "small" -> 12.sp
                            "large" -> 16.sp
                            "extra large" -> 19.sp
                            else -> 14.sp
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF161A26))
                                .border(1.dp, ElectricCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Line 1: I hear Jerusalem bells a-ringin'",
                                    color = TextPrimary,
                                    fontSize = testFontSizeSp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "صدای زنگ‌های اورشلیم را می‌شنوم که به صدا درآمده‌اند",
                                    color = ElectricCyan,
                                    fontSize = testTranslationSizeSp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // ========================================================
            // SECTION 5: LOCAL STORAGE & ROOM CACHE (آکاردئونی)
            // ========================================================
            SettingsAccordionCard(
                title = "حافظه محلی و دیتابیس Room",
                subtitle = "$totalCachedCount آهنگ ذخیره‌شده آفلاین",
                icon = Icons.Default.CleaningServices,
                iconTint = SunsetRose,
                isExpanded = expandedSections.contains("storage"),
                onToggle = { toggleSection("storage") }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "آهنگ‌های قبلاً ترجمه‌شده به طور کامل در دیتابیس آفلاین دستگاه ذخیره می‌شوند و بدون اینترنت با تأخیر 0 میلی‌ثانیه لود می‌شوند.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )

                    Button(
                        onClick = onTestDatabase,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E2536)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, ElectricCyan.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("btn_test_database")
                    ) {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تست سلامت و سرعت دیتابیس Room", color = ElectricCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { showClearConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = SunsetRose.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, SunsetRose.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_btn_clear_cache")
                    ) {
                        Text("پاک‌سازی حافظه کش و بازنشانی آهنگ‌ها", color = SunsetRose, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ========================================================
            // SECTION 6: DIAGNOSTICS & SYSTEM LOGS (ورود سریع)
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, NeonViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .clickable { onNavigateToDiagnostics() }
                    .testTag("settings_btn_open_diagnostics"),
                color = ObsidianSurface
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonViolet.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.BugReport, contentDescription = null, tint = NeonViolet, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("سیستم لاگ و عیب‌یابی زنده", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("مشاهده ریز لاگ‌ها، وضعیت اسپاتیفای و کپی برای چت", color = TextMuted, fontSize = 11.sp)
                        }
                    }
                    Button(
                        onClick = onNavigateToDiagnostics,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("مشاهده", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // ========================================================
            // SECTION 7: ABOUT & ENGINE INFO
            // ========================================================
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.dp, ObsidianBorder, RoundedCornerShape(16.dp)),
                color = Color(0xFF0C101A)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("LyricEcho AI v1.0", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "هوش مصنوعی چند پرووایدره (Google Gemini / OpenAI Proxy)، دیتابیس Room SQLite، پاپ‌آپ شناور سراسری و سیستم سینک لحظه‌ای کارائوکه.",
                            color = TextMuted,
                            fontSize = 11.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsAccordionCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    testTag: String = "",
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                if (isExpanded) iconTint.copy(alpha = 0.45f) else ObsidianBorder,
                RoundedCornerShape(16.dp)
            )
            .testTag(testTag),
        color = androidx.compose.material3.MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(iconTint.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = title,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = subtitle,
                            color = if (isExpanded) iconTint else TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            fontWeight = if (isExpanded) FontWeight.SemiBold else FontWeight.Normal
                        )
                    }
                }

                IconButton(
                    onClick = onToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "بستن" else "باز کردن",
                        tint = if (isExpanded) iconTint else TextMuted
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    content()
                }
            }
        }
    }
}
