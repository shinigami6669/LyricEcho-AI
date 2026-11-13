package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import android.provider.Settings
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.FloatingLyricsOverlay
import com.example.ui.components.PermissionPromptDialog
import com.example.ui.screens.DiagnosticsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.screens.LyricsPlayerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.LyricEchoTheme
import com.example.ui.viewmodel.AppScreen
import com.example.ui.viewmodel.LyricEchoViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    private var spotifyReceiver: com.example.receiver.SpotifyBroadcastReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        spotifyReceiver = com.example.receiver.SpotifyBroadcastReceiver.register(this)
        com.example.service.MusicNotificationListener.rebindService(this)
        setContent {
            val viewModel: LyricEchoViewModel = viewModel()
            val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
            LyricEchoTheme(themeMode = appThemeMode) {
                LyricEchoApp(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        com.example.service.MusicNotificationListener.rebindService(this)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        spotifyReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
        }
    }
}

@Composable
fun LyricEchoApp(
    viewModel: LyricEchoViewModel = viewModel()
) {
    val currentScreen by viewModel.currentScreen.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val filteredSongs by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val allLibrarySongs by viewModel.allLibrarySongs.collectAsStateWithLifecycle()
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val currentLines by viewModel.currentLines.collectAsStateWithLifecycle()
    val isFromCache by viewModel.isFromCache.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val targetLanguage by viewModel.targetLanguage.collectAsStateWithLifecycle()
    val customApiKey by viewModel.customApiKey.collectAsStateWithLifecycle()
    val isAutoScrollEnabled by viewModel.isAutoScrollEnabled.collectAsStateWithLifecycle()
    val isAutoPopupEnabled by viewModel.isAutoPopupEnabled.collectAsStateWithLifecycle()
    val isSpotifyDetectEnabled by viewModel.isSpotifyDetectEnabled.collectAsStateWithLifecycle()
    val isFloatingPopupVisible by viewModel.isFloatingPopupVisible.collectAsStateWithLifecycle()
    val isFloatingPopupCollapsed by viewModel.isFloatingPopupCollapsed.collectAsStateWithLifecycle()
    val playbackSpeed by viewModel.playbackSpeed.collectAsStateWithLifecycle()
    val fontSize by viewModel.fontSize.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val currentPositionMs by viewModel.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by viewModel.durationMs.collectAsStateWithLifecycle()
    val activeLineIndex by viewModel.activeLineIndex.collectAsStateWithLifecycle()
    val isTestingApi by viewModel.isTestingApi.collectAsStateWithLifecycle()
    val apiTestResult by viewModel.apiTestResult.collectAsStateWithLifecycle()
    val syncOffsetMs by viewModel.syncOffsetMs.collectAsStateWithLifecycle()
    val apiEngineSettings by viewModel.apiEngineSettings.collectAsStateWithLifecycle()
    val totalRequests by viewModel.totalRequests.collectAsStateWithLifecycle()
    val successRequests by viewModel.successRequests.collectAsStateWithLifecycle()
    val failedRequests by viewModel.failedRequests.collectAsStateWithLifecycle()
    val isDetectingModels by viewModel.isDetectingModels.collectAsStateWithLifecycle()
    val detectModelsResult by viewModel.detectModelsResult.collectAsStateWithLifecycle()
    val pingResult by viewModel.pingResult.collectAsStateWithLifecycle()
    val isPinging by viewModel.isPinging.collectAsStateWithLifecycle()
    val rawTerminalOutput by viewModel.rawTerminalOutput.collectAsStateWithLifecycle()
    val savedProfiles by viewModel.savedApiProfiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
    val isSystemOverlayActive by com.example.service.FloatingOverlayController.isOverlayActive.collectAsStateWithLifecycle()
    val isAppMasterEnabled by viewModel.isAppMasterEnabled.collectAsStateWithLifecycle()
    val appThemeMode by viewModel.appThemeMode.collectAsStateWithLifecycle()
    val popupScale by viewModel.popupScale.collectAsStateWithLifecycle()
    val popupTapToCollapseEnabled by viewModel.popupTapToCollapseEnabled.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        if (activity?.intent?.getBooleanExtra("OPEN_FLOATING_POPUP", false) == true) {
            viewModel.showFloatingPopup()
        }
    }

    var showPermissionPrompt by remember {
        mutableStateOf(!Settings.canDrawOverlays(context))
    }

    if (showPermissionPrompt) {
        PermissionPromptDialog(onDismiss = { showPermissionPrompt = false })
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) }
        ) { innerPadding ->
            when (currentScreen) {
                AppScreen.HOME -> {
                    HomeScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { viewModel.searchQuery.value = it },
                        songs = filteredSongs,
                        isLoading = isLoading,
                        errorMessage = errorMessage,
                        targetLanguage = targetLanguage,
                        supportedLanguages = viewModel.supportedLanguages,
                        onSelectSong = { song -> viewModel.openSong(song, fromCache = true) },
                        onFetchWithAi = { title, artist ->
                            viewModel.fetchAndOpenSong(title, artist, forceRefresh = false)
                        },
                        onLanguageChange = { lang -> viewModel.setTargetLanguage(lang) },
                        onSaveManualLyric = { title, artist, lang, orig, trans ->
                            viewModel.saveManualLyric(title, artist, lang, orig, trans)
                        },
                        onNavigateToLibrary = { viewModel.navigateTo(AppScreen.LIBRARY) },
                        onNavigateToSettings = { viewModel.navigateTo(AppScreen.SETTINGS) },
                        onNavigateToDiagnostics = { viewModel.navigateTo(AppScreen.DIAGNOSTICS) },
                        onDismissError = { viewModel.clearError() },
                        activeSong = currentSong,
                        isFloatingPopupVisible = isFloatingPopupVisible,
                        onShowFloatingPopup = { viewModel.showFloatingPopup() },
                        apiEngineSettings = apiEngineSettings,
                        isAppMasterEnabled = isAppMasterEnabled,
                        onToggleMasterPower = { viewModel.toggleAppMasterEnabled() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppScreen.LYRICS_PLAYER -> {
                    LyricsPlayerScreen(
                        song = currentSong,
                        lines = currentLines,
                        isFromCache = isFromCache,
                        isPlaying = isPlaying,
                        currentPositionMs = currentPositionMs,
                        durationMs = durationMs,
                        activeLineIndex = activeLineIndex,
                        isAutoScrollEnabled = isAutoScrollEnabled,
                        playbackSpeed = playbackSpeed,
                        fontSize = fontSize,
                        targetLanguage = targetLanguage,
                        supportedLanguages = viewModel.supportedLanguages,
                        isLoading = isLoading,
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                        onSeek = { ms -> viewModel.playbackManager.seekTo(ms) },
                        onSeekToLine = { line -> viewModel.seekToLine(line) },
                        onToggleAutoScroll = { viewModel.toggleAutoScroll() },
                        onChangeSpeed = { spd -> viewModel.setPlaybackSpeed(spd) },
                        onCycleFontSize = {
                            val next = when (fontSize) {
                                "Small" -> "Medium"
                                "Medium" -> "Large"
                                "Large" -> "Extra Large"
                                else -> "Small"
                            }
                            viewModel.setFontSize(next)
                        },
                        onRetranslate = { newLang -> viewModel.retranslateCurrentSong(newLang) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onPopOutFloating = {
                            viewModel.showFloatingPopup()
                            viewModel.navigateTo(AppScreen.HOME)
                        },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppScreen.LIBRARY -> {
                    LibraryScreen(
                        songs = allLibrarySongs,
                        onSelectSong = { song -> viewModel.openSong(song, fromCache = true) },
                        onToggleFavorite = { song -> viewModel.toggleFavorite(song) },
                        onDeleteSong = { song -> viewModel.deleteSong(song) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppScreen.SETTINGS -> {
                    SettingsScreen(
                        currentTargetLanguage = targetLanguage,
                        supportedLanguages = viewModel.supportedLanguages,
                        currentApiKey = customApiKey,
                        currentFontSize = fontSize,
                        isAutoScrollEnabled = isAutoScrollEnabled,
                        isAutoPopupEnabled = isAutoPopupEnabled,
                        isSpotifyDetectEnabled = isSpotifyDetectEnabled,
                        totalCachedCount = allLibrarySongs.size,
                        apiEngineSettings = apiEngineSettings,
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
                        onLanguageChange = { lang -> viewModel.setTargetLanguage(lang) },
                        onApiKeyChange = { key -> viewModel.setCustomApiKey(key) },
                        onFontSizeChange = { sz -> viewModel.setFontSize(sz) },
                        onToggleAutoScroll = { viewModel.toggleAutoScroll() },
                        onToggleAutoPopup = { viewModel.setAutoPopupEnabled(!isAutoPopupEnabled) },
                        onToggleSpotifyDetect = { viewModel.setSpotifyDetectEnabled(!isSpotifyDetectEnabled) },
                        onSimulateSpotify = { track, artist -> viewModel.simulateSpotifySong(track, artist) },
                        onProviderTypeChange = { type -> viewModel.setApiProviderType(type) },
                        onBaseUrlChange = { url -> viewModel.setApiBaseUrl(url) },
                        onModelChange = { model -> viewModel.setSelectedModel(model) },
                        onMaxRetriesChange = { retries -> viewModel.setMaxRetries(retries) },
                        onTimeoutSecondsChange = { timeout -> viewModel.setTimeoutSeconds(timeout) },
                        onRpmLimitChange = { rpm -> viewModel.setRpmLimit(rpm) },
                        onAutoFallbackChange = { fallback -> viewModel.setAutoFallbackToGemini(fallback) },
                        onBackupKeyChange = { key -> viewModel.setBackupGeminiKey(key) },
                        onDetectModels = { viewModel.detectAvailableModels() },
                        onPingEndpoint = { viewModel.pingEndpoint() },
                        onResetStats = { viewModel.resetRequestStats() },
                        onTestApiKey = { key -> viewModel.testGeminiApi(key) },
                        onCancelTestApi = { viewModel.cancelTestApi() },
                        onClearTerminal = { viewModel.clearTerminal() },
                        onSaveProfile = { name -> viewModel.saveCurrentAsProfile(name) },
                        onDeleteProfile = { id -> viewModel.deleteApiProfile(id) },
                        onActivateProfile = { profile -> viewModel.activateApiProfile(profile) },
                        onTestFloatingPopup = { viewModel.testFloatingLyricsPopup() },
                        onTestDatabase = { viewModel.testDatabaseSpeed() },
                        onClearCache = { viewModel.clearCache() },
                        onNavigateToDiagnostics = { viewModel.navigateTo(AppScreen.DIAGNOSTICS) },
                        isAppMasterEnabled = isAppMasterEnabled,
                        onToggleMasterPower = { viewModel.toggleAppMasterEnabled() },
                        currentThemeMode = appThemeMode,
                        onThemeChange = { viewModel.setAppTheme(it) },
                        popupScale = popupScale,
                        onPopupScaleChange = { viewModel.setPopupScale(it) },
                        popupTapToCollapseEnabled = popupTapToCollapseEnabled,
                        onTogglePopupTapToCollapse = { viewModel.setPopupTapToCollapse(!popupTapToCollapseEnabled) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }

                AppScreen.DIAGNOSTICS -> {
                    DiagnosticsScreen(
                        onSimulateSpotify = { track, artist -> viewModel.simulateSpotifySong(track, artist) },
                        onTestNotification = { track, artist -> viewModel.testSimulateNotification(track, artist) },
                        onTestGeminiApi = { viewModel.testGeminiApi(customApiKey) },
                        onBack = { viewModel.navigateTo(AppScreen.HOME) },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }

        // Musixmatch-Style Floating Draggable Lyrics Popup:
        // Strictly single-popup architecture: if system overlay permission is granted, use the system-wide window;
        // otherwise fall back to the in-app Compose overlay so two popups are NEVER rendered simultaneously.
        val canDrawSystemOverlay = Settings.canDrawOverlays(context)
        LaunchedEffect(isFloatingPopupVisible, currentSong, isAppMasterEnabled) {
            if (isFloatingPopupVisible && currentSong != null && isAppMasterEnabled && canDrawSystemOverlay && !isSystemOverlayActive) {
                com.example.service.FloatingOverlayController.showSystemOverlay(context)
            }
        }

        if (isFloatingPopupVisible && currentSong != null && currentScreen != AppScreen.LYRICS_PLAYER && !isSystemOverlayActive && !canDrawSystemOverlay) {
            FloatingLyricsOverlay(
                song = currentSong,
                lines = currentLines,
                activeLineIndex = activeLineIndex,
                isPlaying = isPlaying,
                currentPositionMs = currentPositionMs,
                durationMs = durationMs,
                targetLanguage = currentSong?.language ?: targetLanguage,
                isCollapsed = isFloatingPopupCollapsed,
                onToggleCollapse = { viewModel.toggleFloatingPopupCollapse() },
                onExpandToFullScreen = { viewModel.navigateTo(AppScreen.LYRICS_PLAYER) },
                onClose = { viewModel.hideFloatingPopup() },
                onTogglePlayPause = { viewModel.playbackManager.togglePlayPause() },
                onSeekRelative = { delta ->
                    viewModel.playbackManager.seekTo((currentPositionMs + delta).coerceIn(0L, durationMs))
                },
                syncOffsetMs = syncOffsetMs,
                onAdjustSyncOffset = { delta -> viewModel.adjustSyncOffset(delta) },
                onResetSyncOffset = { viewModel.resetSyncOffset() },
                onRefresh = { currentSong?.let { viewModel.refreshSongWithActiveModel(it.title, it.artist) } },
                popupScale = popupScale,
                tapToCollapseEnabled = popupTapToCollapseEnabled,
                themeMode = appThemeMode,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 70.dp)
            )
        }
    }
}
