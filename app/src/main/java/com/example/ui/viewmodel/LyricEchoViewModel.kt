package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.LyricLine
import com.example.data.model.SongLyric
import com.example.data.preferences.SettingsDataStore
import com.example.data.remote.ApiEngineSettings
import com.example.data.remote.ApiProviderType
import com.example.data.remote.EndpointPingResult
import com.example.data.repository.LyricRepository
import com.example.player.AudioPlaybackManager
import com.example.receiver.SpotifyMediaManager
import com.example.receiver.SpotifyTrackEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppScreen {
    HOME,
    LYRICS_PLAYER,
    LIBRARY,
    SETTINGS,
    DIAGNOSTICS
}

class LyricEchoViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    val repository = LyricRepository(db.lyricDao())
    val settingsDataStore = SettingsDataStore(application)
    val playbackManager = AudioPlaybackManager(application, viewModelScope)

    // Current navigation screen
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Search query
    val searchQuery = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredSongs: StateFlow<List<SongLyric>> = searchQuery.flatMapLatest { query ->
        if (query.isBlank()) {
            repository.recentSongs
        } else {
            repository.searchSongs(query)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allLibrarySongs: StateFlow<List<SongLyric>> = repository.allSongs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active song being viewed/played
    private val _currentSong = MutableStateFlow<SongLyric?>(null)
    val currentSong: StateFlow<SongLyric?> = _currentSong.asStateFlow()

    // Lines for the active song
    private val _currentLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLines: StateFlow<List<LyricLine>> = _currentLines.asStateFlow()

    // Offline cache vs AI Live indicator
    private val _isFromCache = MutableStateFlow(true)
    val isFromCache: StateFlow<Boolean> = _isFromCache.asStateFlow()

    // Loading & Error States
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // User preferences
    val targetLanguage = settingsDataStore.targetLanguage
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Persian")

    val customApiKey = settingsDataStore.customApiKey
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val isAutoScrollEnabled = settingsDataStore.isAutoScrollEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val playbackSpeed = settingsDataStore.playbackSpeed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 1.0f)

    val fontSize = settingsDataStore.fontSize
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Medium")

    val isAutoPopupEnabled = settingsDataStore.isAutoPopupEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isSpotifyDetectEnabled = settingsDataStore.isSpotifyDetectEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val isAppMasterEnabled = settingsDataStore.isAppMasterEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val appThemeMode = settingsDataStore.appThemeMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.ui.theme.AppThemeMode.NEON_OBSIDIAN)

    val popupScale = settingsDataStore.popupScale
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Medium")

    val popupTapToCollapseEnabled = settingsDataStore.popupTapToCollapseEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val apiEngineSettings = settingsDataStore.apiEngineSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ApiEngineSettings())

    val totalRequests = settingsDataStore.totalRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successRequests = settingsDataStore.successRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedRequests = settingsDataStore.failedRequests
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val _isDetectingModels = MutableStateFlow(false)
    val isDetectingModels: StateFlow<Boolean> = _isDetectingModels.asStateFlow()

    private val _detectModelsResult = MutableStateFlow<String?>(null)
    val detectModelsResult: StateFlow<String?> = _detectModelsResult.asStateFlow()

    private val _pingResult = MutableStateFlow<EndpointPingResult?>(null)
    val pingResult: StateFlow<EndpointPingResult?> = _pingResult.asStateFlow()

    private val _isPinging = MutableStateFlow(false)
    val isPinging: StateFlow<Boolean> = _isPinging.asStateFlow()

    private val _rawTerminalOutput = MutableStateFlow(
        "> LyricEcho AI Engine Ready.\n> Configure your API key in Settings to start real-time translation."
    )
    val rawTerminalOutput: StateFlow<String> = _rawTerminalOutput.asStateFlow()

    fun clearTerminal() {
        _rawTerminalOutput.value = "> Terminal cleared.\n> Ready for new requests."
    }

    val savedApiProfiles = settingsDataStore.savedApiProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.remote.ApiProfile.getDefaultProfiles())

    val activeProfileId = settingsDataStore.activeProfileId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "preset_gemini_official")

    fun saveCurrentAsProfile(profileName: String) {
        val current = apiEngineSettings.value
        val newProfile = com.example.data.remote.ApiProfile(
            id = "custom_${System.currentTimeMillis()}",
            name = profileName.trim().ifBlank { "پروفایل سفارشی" },
            providerTypeId = current.providerType.id,
            baseUrl = current.baseUrl,
            apiKey = current.apiKey,
            selectedModel = current.selectedModel,
            timeoutSeconds = current.timeoutSeconds,
            maxRetries = current.maxRetries,
            rpmLimit = current.requestsPerMinute,
            isSystemPreset = false
        )
        viewModelScope.launch {
            settingsDataStore.saveApiProfile(newProfile)
            settingsDataStore.activateApiProfile(newProfile)
            _snackbarMessage.emit("پروفایل «${newProfile.name}» ذخیره و فعال شد.")
            com.example.diagnostics.AppLogger.s(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "پروفایل جدید API ذخیره گردید: ${newProfile.name}",
                "BaseUrl: ${newProfile.baseUrl} | Model: ${newProfile.selectedModel}"
            )
        }
    }

    fun deleteApiProfile(profileId: String) {
        viewModelScope.launch {
            settingsDataStore.deleteApiProfile(profileId)
            _snackbarMessage.emit("پروفایل حذف شد.")
        }
    }

    fun activateApiProfile(profile: com.example.data.remote.ApiProfile) {
        viewModelScope.launch {
            settingsDataStore.activateApiProfile(profile)
            com.example.service.FloatingOverlayController.setActiveModel(profile.selectedModel)
            _snackbarMessage.emit("پروفایل «${profile.name}» (مدل: ${profile.selectedModel}) فعال شد.")
            com.example.diagnostics.AppLogger.i(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "سوئیچ به پروفایل API: ${profile.name}",
                "Provider: ${profile.providerTypeId} | URL: ${profile.baseUrl} | Model: ${profile.selectedModel}"
            )
        }
    }

    private val _isFloatingPopupVisible = MutableStateFlow(false)
    val isFloatingPopupVisible: StateFlow<Boolean> = _isFloatingPopupVisible.asStateFlow()

    private val _isFloatingPopupCollapsed = MutableStateFlow(false)
    val isFloatingPopupCollapsed: StateFlow<Boolean> = _isFloatingPopupCollapsed.asStateFlow()

    // Playback state forwarded from AudioPlaybackManager
    val isPlaying = playbackManager.isPlaying
    val currentPositionMs = playbackManager.currentPositionMs
    val durationMs = playbackManager.durationMs

    val syncOffsetMs = com.example.service.FloatingOverlayController.syncOffsetMs

    fun adjustSyncOffset(deltaMs: Long) {
        com.example.service.FloatingOverlayController.adjustSyncOffset(deltaMs)
    }

    fun resetSyncOffset() {
        com.example.service.FloatingOverlayController.resetSyncOffset()
    }

    // Active line index calculated from (currentPositionMs + syncOffsetMs)
    val activeLineIndex: StateFlow<Int> = combine(currentPositionMs, _currentLines, syncOffsetMs) { pos, lines, offset ->
        if (lines.isEmpty()) -1
        else {
            val eff = (pos + offset).coerceAtLeast(0L)
            val idx = lines.indexOfLast { it.timeMs <= eff }
            if (idx == -1) 0 else idx
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val supportedLanguages = listOf(
        "Persian",
        "Spanish",
        "French",
        "German",
        "Japanese",
        "Arabic",
        "Turkish",
        "Italian",
        "Korean",
        "Russian"
    )

    init {
        repository.aiService.onTerminalLog = { logLine ->
            val prev = _rawTerminalOutput.value
            _rawTerminalOutput.value = if (prev.length > 8000) {
                prev.takeLast(6000) + "\n$logLine"
            } else {
                "$prev\n$logLine"
            }
        }

        // One-time production release cleanup: clears any previous dev/test API keys and test cache
        viewModelScope.launch {
            val cleaned = settingsDataStore.purgeLegacyTestCredentialsIfNeeded()
            if (cleaned) {
                repository.clearCache()
            }
        }

        // Keep FloatingOverlayController active model synced with selected profile/model
        viewModelScope.launch {
            apiEngineSettings.collectLatest { s ->
                com.example.service.FloatingOverlayController.setActiveModel(s.selectedModel)
            }
        }

        // Keep FloatingOverlayController updated with playback time & duration
        viewModelScope.launch {
            playbackManager.currentPositionMs.collectLatest { pos ->
                com.example.service.FloatingOverlayController.updatePosition(pos)
            }
        }
        viewModelScope.launch {
            playbackManager.durationMs.collectLatest { dur ->
                if (dur > 0) {
                    com.example.service.FloatingOverlayController.updatePosition(
                        posMs = playbackManager.currentPositionMs.value,
                        durationMs = dur
                    )
                }
            }
        }
        viewModelScope.launch {
            playbackManager.isPlaying.collectLatest { playing ->
                com.example.service.FloatingOverlayController.updatePosition(
                    posMs = playbackManager.currentPositionMs.value,
                    isPlaying = playing
                )
            }
        }
        com.example.service.FloatingOverlayController.onPlayPauseCallback = {
            playbackManager.togglePlayPause()
        }
        com.example.service.FloatingOverlayController.onSeekCallback = { ms ->
            playbackManager.seekTo(ms)
        }
        com.example.service.FloatingOverlayController.onRefreshCallback = {
            _currentSong.value?.let { s ->
                refreshSongWithActiveModel(s.title, s.artist)
            }
        }
        com.example.service.FloatingOverlayController.onUserDismissOverlayCallback = {
            userDismissedPopupForSongTitle = _currentSong.value?.title
            _isFloatingPopupVisible.value = false
        }

        // Automatically show Musixmatch-style floating popup when playing and user is browsing other screens
        viewModelScope.launch {
            combine(playbackManager.isPlaying, isAutoPopupEnabled, _currentScreen) { playing, autoPopup, screen ->
                val curTitle = _currentSong.value?.title
                if (playing && autoPopup && screen != AppScreen.LYRICS_PLAYER && curTitle != null &&
                    !curTitle.equals(userDismissedPopupForSongTitle, ignoreCase = true)
                ) {
                    _isFloatingPopupVisible.value = true
                }
            }.collectLatest {}
        }

        viewModelScope.launch {
            isAppMasterEnabled.collectLatest { enabled ->
                com.example.service.FloatingOverlayController.setMasterEnabled(enabled)
            }
        }
        viewModelScope.launch {
            appThemeMode.collectLatest { theme ->
                com.example.service.FloatingOverlayController.setAppThemeMode(theme)
            }
        }
        viewModelScope.launch {
            popupScale.collectLatest { scale ->
                com.example.service.FloatingOverlayController.setPopupScale(scale)
            }
        }
        viewModelScope.launch {
            popupTapToCollapseEnabled.collectLatest { tapEnabled ->
                com.example.service.FloatingOverlayController.setTapToCollapseEnabled(tapEnabled)
            }
        }

        // Real-time Spotify music playback detector like Musixmatch
        viewModelScope.launch {
            SpotifyMediaManager.trackChangeEvent.collectLatest { event ->
                com.example.diagnostics.AppLogger.i(
                    com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                    "دریافت رویداد ترک در ViewModel: \"${event.track}\" - ${event.artist}",
                    "IsPlaying=${event.isPlaying} | Position=${event.positionMs}ms | AutoDetectEnabled=${isSpotifyDetectEnabled.value}"
                )
                if (!isAppMasterEnabled.value) {
                    com.example.diagnostics.AppLogger.w(
                        com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                        "سیستم اصلی برنامه خاموش (OFF) است. ترک اسپاتیفای نادیده گرفته شد."
                    )
                } else if (isSpotifyDetectEnabled.value && event.isPlaying) {
                    handleSpotifyTrack(event)
                } else if (!isSpotifyDetectEnabled.value) {
                    com.example.diagnostics.AppLogger.w(
                        com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                        "تشخیص خودکار اسپاتیفای در تنظیمات خاموش است! پاپ‌آپ باز نمی‌شود (Enable in Settings)"
                    )
                }
            }
        }
    }

    private var userDismissedPopupForSongTitle: String? = null

    private fun handleSpotifyTrack(event: SpotifyTrackEvent) {
        if (!isAppMasterEnabled.value) return
        val current = _currentSong.value
        if (current != null && current.title.equals(event.track, ignoreCase = true)) {
            com.example.diagnostics.AppLogger.i(
                com.example.diagnostics.LogTag.OVERLAY_POPUP,
                "آهنگ \"${event.track}\" از قبل باز است. همگام‌سازی موقعیت",
                "Position: ${event.positionMs}ms"
            )
            playbackManager.seekTo(event.positionMs)
            playbackManager.play()
            com.example.service.FloatingOverlayController.updatePosition(event.positionMs, true, durationMs = event.durationMs)
            if (!event.track.equals(userDismissedPopupForSongTitle, ignoreCase = true)) {
                com.example.service.FloatingOverlayController.showSystemOverlay(getApplication())
                _isFloatingPopupVisible.value = true
                _isFloatingPopupCollapsed.value = false
            }
            return
        }
        userDismissedPopupForSongTitle = null

        com.example.diagnostics.AppLogger.s(
            com.example.diagnostics.LogTag.OVERLAY_POPUP,
            "رویداد پخش آهنگ دریافت شد: \"${event.track}\" - ${event.artist}",
            "Position: ${event.positionMs}ms | Auto-opening pop-up immediately..."
        )

        val lang = targetLanguage.value
        val initialLines = listOf(
            LyricLine(0, 0L, event.track, "🎵 در حال دریافت و ترجمه هم‌زمان متن با هوش مصنوعی...")
        )
        val placeholderSong = SongLyric(
            title = event.track,
            artist = event.artist,
            language = lang,
            originalLyrics = "${event.track}\n${event.artist}",
            translatedLyrics = "در حال ترجمه با هوش مصنوعی...",
            linesJson = SongLyric.serializeLines(initialLines),
            durationMs = if (event.durationMs > 0) event.durationMs else 180000L
        )

        // 1. INSTANT FEEDBACK: Show the pop-up immediately in under 50ms!
        _currentSong.value = placeholderSong
        _currentLines.value = initialLines
        playbackManager.setSongDuration(placeholderSong.durationMs)
        playbackManager.seekTo(event.positionMs)
        playbackManager.play()

        com.example.service.FloatingOverlayController.updateSong(
            song = placeholderSong,
            lines = initialLines,
            startPositionMs = event.positionMs,
            playing = true,
            explicitDurationMs = placeholderSong.durationMs
        )
        com.example.service.FloatingOverlayController.showSystemOverlay(getApplication())
        _isFloatingPopupVisible.value = true
        _isFloatingPopupCollapsed.value = false

        viewModelScope.launch {
            _snackbarMessage.emit("Spotify: «${event.track}» تشخیص داده شد - پاپ‌آپ باز شد!")
        }

        // 2. BACKGROUND FETCH: Query Room local cache or fetch with AI
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getSong(
                title = event.track,
                artist = event.artist,
                language = lang,
                apiSettings = apiEngineSettings.value,
                userCustomKey = customApiKey.value,
                forceRefresh = false
            )
            _isLoading.value = false
            settingsDataStore.recordRequestResult(result.isSuccess)
            result.fold(
                onSuccess = { response ->
                    _currentSong.value = response.songLyric
                    _currentLines.value = response.songLyric.getLines()
                    _isFromCache.value = response.isFromCache

                    // Smoothly update running overlay with real lines
                    com.example.service.FloatingOverlayController.updateSong(
                        song = response.songLyric,
                        lines = response.songLyric.getLines(),
                        startPositionMs = playbackManager.currentPositionMs.value,
                        playing = true,
                        explicitDurationMs = if (event.durationMs > 0) event.durationMs else response.songLyric.durationMs
                    )

                    com.example.diagnostics.AppLogger.s(
                        com.example.diagnostics.LogTag.OVERLAY_POPUP,
                        "متن لیریک با موفقیت روی پاپ‌آپ شناور تزریق گردید",
                        "Song: \"${event.track}\" | Lines: ${response.songLyric.getLines().size} | FromCache: ${response.isFromCache}"
                    )
                    com.example.service.MediaNotificationHelper.showSpotifySyncNotification(
                        getApplication(),
                        response.songLyric.title,
                        response.songLyric.artist
                    )
                },
                onFailure = { err ->
                    val failureLines = listOf(
                        LyricLine(0, 0L, event.track, "⚠️ خطا در ارتباط با هوش مصنوعی (${err.message}). برای تست سرور به تنظیمات بروید.")
                    )
                    val errorSong = placeholderSong.copy(
                        linesJson = SongLyric.serializeLines(failureLines)
                    )
                    _currentSong.value = errorSong
                    _currentLines.value = failureLines
                    com.example.service.FloatingOverlayController.updateSong(
                        song = errorSong,
                        lines = failureLines,
                        startPositionMs = playbackManager.currentPositionMs.value,
                        playing = true
                    )
                    _errorMessage.value = "Spotify song: ${err.message}"
                    com.example.diagnostics.AppLogger.e(
                        com.example.diagnostics.LogTag.OVERLAY_POPUP,
                        "خطا در دریافت متن از سرور برای پاپ‌آپ: ${err.message}",
                        "Song: ${event.track} by ${event.artist}"
                    )
                }
            )
        }
    }

    fun testSimulateNotification(track: String, artist: String) {
        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.NOTIFICATION_LISTENER,
            "تست دستی شبیه‌سازی نوتیفیکیشن اسپاتیفای: \"$track\" - $artist"
        )
        SpotifyMediaManager.onTrackReceived(track, artist, true, 0L, 180000L)
    }

    fun simulateSpotifySong(track: String, artist: String) {
        SpotifyMediaManager.simulateSpotifyBroadcast(getApplication(), track, artist)
    }

    fun setSpotifyDetectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setSpotifyDetectEnabled(enabled)
            _snackbarMessage.emit(
                if (enabled) "Spotify auto-detection enabled!"
                else "Spotify auto-detection disabled"
            )
        }
    }

    fun showFloatingPopup() {
        userDismissedPopupForSongTitle = null
        val song = _currentSong.value ?: allLibrarySongs.value.firstOrNull()
        if (song != null) {
            _currentSong.value = song
            _currentLines.value = song.getLines()
            _isFloatingPopupVisible.value = true
            _isFloatingPopupCollapsed.value = false
            playbackManager.setSongDuration(if (song.durationMs > 0) song.durationMs else 180000L)
            playbackManager.play()
            com.example.service.FloatingOverlayController.updateSong(
                song = song,
                lines = song.getLines(),
                startPositionMs = playbackManager.currentPositionMs.value,
                playing = true
            )
            com.example.service.FloatingOverlayController.showSystemOverlay(getApplication())
        } else {
            viewModelScope.launch {
                _snackbarMessage.emit("هم‌اکنون آهنگی در حال پخش نیست. یک آهنگ در اسپاتیفای پخش کنید یا از کتابخانه انتخاب نمایید.")
            }
        }
    }

    fun hideFloatingPopup() {
        userDismissedPopupForSongTitle = _currentSong.value?.title
        _isFloatingPopupVisible.value = false
        com.example.service.FloatingOverlayController.hideSystemOverlay(getApplication())
    }

    fun toggleFloatingPopupCollapse() {
        _isFloatingPopupCollapsed.value = !_isFloatingPopupCollapsed.value
        com.example.service.FloatingOverlayController.toggleCollapse()
    }

    fun setAutoPopupEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoPopupEnabled(enabled)
            _snackbarMessage.emit(
                if (enabled) "Musixmatch Floating Pop-up auto-show enabled!" 
                else "Floating Pop-up auto-show disabled"
            )
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen == AppScreen.LYRICS_PLAYER) {
            com.example.service.FloatingOverlayController.setCollapsed(true)
        }
    }

    fun openSong(song: SongLyric, fromCache: Boolean = true) {
        _currentSong.value = song
        _currentLines.value = song.getLines()
        _isFromCache.value = fromCache
        playbackManager.setSongDuration(song.durationMs)
        playbackManager.seekTo(0L)
        com.example.service.FloatingOverlayController.updateSong(
            song = song,
            lines = song.getLines(),
            startPositionMs = 0L,
            playing = playbackManager.isPlaying.value
        )
        _currentScreen.value = AppScreen.LYRICS_PLAYER
    }

    fun fetchAndOpenSong(title: String, artist: String, forceRefresh: Boolean = false) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val lang = targetLanguage.value
            val customKey = customApiKey.value

            val result = repository.getSong(
                title = title,
                artist = artist,
                language = lang,
                apiSettings = apiEngineSettings.value,
                userCustomKey = customKey,
                forceRefresh = forceRefresh
            )

            _isLoading.value = false
            settingsDataStore.recordRequestResult(result.isSuccess)
            result.fold(
                onSuccess = { response ->
                    _currentSong.value = response.songLyric
                    _currentLines.value = response.songLyric.getLines()
                    _isFromCache.value = response.isFromCache
                    playbackManager.setSongDuration(response.songLyric.durationMs)
                    playbackManager.seekTo(0L)
                    _currentScreen.value = AppScreen.LYRICS_PLAYER
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to retrieve song lyrics."
                }
            )
        }
    }

    fun refreshSongWithActiveModel(title: String, artist: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _isLoading.value = true
            val lang = targetLanguage.value
            val customKey = customApiKey.value
            val activeModel = apiEngineSettings.value.selectedModel

            _snackbarMessage.emit("در حال ترجمه مجدد با مدل «$activeModel»...")
            com.example.diagnostics.AppLogger.i(
                com.example.diagnostics.LogTag.GEMINI_AI,
                "درخواست ترجمه مجدد آهنگ «$title» با مدل فعال: $activeModel"
            )

            val result = repository.getSong(
                title = title,
                artist = artist,
                language = lang,
                apiSettings = apiEngineSettings.value,
                userCustomKey = customKey,
                forceRefresh = true
            )

            _isLoading.value = false
            settingsDataStore.recordRequestResult(result.isSuccess)
            result.fold(
                onSuccess = { response ->
                    _currentSong.value = response.songLyric
                    _currentLines.value = response.songLyric.getLines()
                    _isFromCache.value = response.isFromCache
                    com.example.service.FloatingOverlayController.updateSong(
                        song = response.songLyric,
                        lines = response.songLyric.getLines(),
                        startPositionMs = playbackManager.currentPositionMs.value,
                        playing = playbackManager.isPlaying.value,
                        explicitDurationMs = response.songLyric.durationMs
                    )
                    _snackbarMessage.emit("متن «$title» با مدل «$activeModel» با موفقیت ترجمه شد.")
                },
                onFailure = { err ->
                    _errorMessage.value = err.message
                    _snackbarMessage.emit("خطا در ترجمه با $activeModel: ${err.message}")
                }
            )
        }
    }

    fun retranslateCurrentSong(newLanguage: String) {
        val song = _currentSong.value ?: return
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            setTargetLanguage(newLanguage)

            val result = repository.getSong(
                title = song.title,
                artist = song.artist,
                language = newLanguage,
                apiSettings = apiEngineSettings.value,
                userCustomKey = customApiKey.value,
                forceRefresh = true
            )

            _isLoading.value = false
            settingsDataStore.recordRequestResult(result.isSuccess)
            result.fold(
                onSuccess = { response ->
                    _currentSong.value = response.songLyric
                    _currentLines.value = response.songLyric.getLines()
                    _isFromCache.value = response.isFromCache
                    _snackbarMessage.emit("Translated to $newLanguage with Gemini AI")
                },
                onFailure = { err ->
                    _errorMessage.value = err.message ?: "Failed to re-translate."
                }
            )
        }
    }

    fun saveManualLyric(
        title: String,
        artist: String,
        language: String,
        originalLyrics: String,
        translatedLyrics: String
    ) {
        viewModelScope.launch {
            val saved = repository.saveManualLyric(
                title = title,
                artist = artist,
                language = language,
                originalLyrics = originalLyrics,
                translatedLyrics = translatedLyrics
            )
            openSong(saved, fromCache = true)
            _snackbarMessage.emit("Lyrics saved to local offline library!")
        }
    }

    fun seekToLine(line: LyricLine) {
        playbackManager.seekTo(line.timeMs)
    }

    fun toggleFavorite(song: SongLyric) {
        viewModelScope.launch {
            repository.toggleFavorite(song.id, song.isFavorite)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = _currentSong.value?.copy(isFavorite = !song.isFavorite)
            }
        }
    }

    fun deleteSong(song: SongLyric) {
        viewModelScope.launch {
            repository.deleteSong(song)
            if (_currentSong.value?.id == song.id) {
                _currentSong.value = null
                _currentScreen.value = AppScreen.HOME
            }
            _snackbarMessage.emit("Removed \"${song.title}\" from saved library")
        }
    }

    fun setTargetLanguage(language: String) {
        viewModelScope.launch {
            settingsDataStore.setTargetLanguage(language)
        }
    }

    fun setCustomApiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setCustomApiKey(key)
            _snackbarMessage.emit("Gemini API key updated!")
        }
    }

    fun toggleAutoScroll() {
        viewModelScope.launch {
            val newVal = !isAutoScrollEnabled.value
            settingsDataStore.setAutoScrollEnabled(newVal)
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        viewModelScope.launch {
            settingsDataStore.setPlaybackSpeed(speed)
            playbackManager.setSpeed(speed)
        }
    }

    fun setFontSize(size: String) {
        viewModelScope.launch {
            settingsDataStore.setFontSize(size)
        }
    }

    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            _snackbarMessage.emit("Cache reset and initial library restored.")
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun setApiProviderType(type: ApiProviderType) {
        viewModelScope.launch {
            settingsDataStore.setApiProviderType(type)
            _snackbarMessage.emit("پرووایدر به ${type.displayName} تغییر یافت")
        }
    }

    fun setApiBaseUrl(url: String) {
        viewModelScope.launch {
            settingsDataStore.setApiBaseUrl(url)
        }
    }

    fun setSelectedModel(model: String) {
        viewModelScope.launch {
            settingsDataStore.setSelectedModel(model)
            _snackbarMessage.emit("مدل فعال: $model")
        }
    }

    fun setMaxRetries(retries: Int) {
        viewModelScope.launch {
            settingsDataStore.setMaxRetries(retries)
        }
    }

    fun setTimeoutSeconds(seconds: Int) {
        viewModelScope.launch {
            settingsDataStore.setTimeoutSeconds(seconds)
        }
    }

    fun setRpmLimit(rpm: Int) {
        viewModelScope.launch {
            settingsDataStore.setRpmLimit(rpm)
        }
    }

    fun setAutoFallbackToGemini(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setAutoFallbackToGemini(enabled)
        }
    }

    fun setBackupGeminiKey(key: String) {
        viewModelScope.launch {
            settingsDataStore.setBackupGeminiKey(key)
        }
    }

    fun setTemperature(temp: Float) {
        viewModelScope.launch {
            settingsDataStore.setTemperature(temp)
        }
    }

    fun resetRequestStats() {
        viewModelScope.launch {
            settingsDataStore.resetRequestStats()
            _snackbarMessage.emit("آمار درخواست‌ها ریست شد.")
        }
    }

    fun detectAvailableModels() {
        viewModelScope.launch {
            _isDetectingModels.value = true
            _detectModelsResult.value = null
            val currentSettings = apiEngineSettings.value
            val isGemini = currentSettings.providerType.isGeminiNative
            val result = repository.aiService.detectAvailableModels(
                baseUrl = currentSettings.baseUrl,
                apiKey = currentSettings.apiKey,
                isGemini = isGemini
            )
            _isDetectingModels.value = false
            result.fold(
                onSuccess = { models ->
                    settingsDataStore.setDiscoveredModels(models)
                    if (models.isNotEmpty() && !models.contains(currentSettings.selectedModel)) {
                        settingsDataStore.setSelectedModel(models.first())
                    }
                    val msg = "موفق: ${models.size} مدل در دسترس شناسایی گردید!"
                    _detectModelsResult.value = msg
                    _snackbarMessage.emit(msg)
                },
                onFailure = { err ->
                    val msg = "خطا در شناسایی مدل‌ها: ${err.message}"
                    _detectModelsResult.value = msg
                    _snackbarMessage.emit(msg)
                }
            )
        }
    }

    fun pingEndpoint() {
        viewModelScope.launch {
            _isPinging.value = true
            _pingResult.value = null
            val currentSettings = apiEngineSettings.value
            val isGemini = currentSettings.providerType.isGeminiNative
            val result = repository.aiService.pingEndpoint(
                baseUrl = currentSettings.baseUrl,
                apiKey = currentSettings.apiKey,
                isGemini = isGemini
            )
            _isPinging.value = false
            result.fold(
                onSuccess = { ping ->
                    _pingResult.value = ping
                    _snackbarMessage.emit(ping.message)
                },
                onFailure = { err ->
                    val failure = EndpointPingResult(
                        isSuccess = false,
                        latencyMs = -1,
                        httpCode = 0,
                        message = err.message ?: "پینگ ناموفق بود"
                    )
                    _pingResult.value = failure
                    _snackbarMessage.emit("خطا در تست تاخیر: ${err.message}")
                }
            )
        }
    }

    private val _isTestingApi = MutableStateFlow(false)
    val isTestingApi: StateFlow<Boolean> = _isTestingApi.asStateFlow()

    private val _apiTestResult = MutableStateFlow<String?>(null)
    val apiTestResult: StateFlow<String?> = _apiTestResult.asStateFlow()

    private var testApiJob: kotlinx.coroutines.Job? = null

    fun testGeminiApi(key: String) {
        testApiJob?.cancel()
        testApiJob = viewModelScope.launch {
            _isTestingApi.value = true
            _apiTestResult.value = null
            _rawTerminalOutput.value = "> [START] تست اتصال و تولید لیریک با هوش مصنوعی..."
            val currentSettings = apiEngineSettings.value
            val testSettings = if (key.isNotBlank()) currentSettings.copy(apiKey = key) else currentSettings

            try {
                val result = repository.getSong(
                    title = "Imagine",
                    artist = "John Lennon",
                    language = targetLanguage.value,
                    apiSettings = testSettings,
                    userCustomKey = testSettings.apiKey,
                    forceRefresh = true
                )
                _isTestingApi.value = false
                settingsDataStore.recordRequestResult(result.isSuccess)
                result.fold(
                    onSuccess = { resp ->
                        _apiTestResult.value = "اتصال موفق به API! دریافت لیریک با موفقیت انجام شد (${resp.songLyric.getLines().size} خط متن)"
                        _rawTerminalOutput.value += "\n✔ [SUCCESS] دریافت و اعتبارسنجی ${resp.songLyric.getLines().size} خط لیریک با موفقیت انجام شد."
                        _snackbarMessage.emit("تست موفق: اتصال و استخراج با هوش مصنوعی کاملاً فعال است!")
                    },
                    onFailure = { err ->
                        _apiTestResult.value = "خطا در اتصال: ${err.message}"
                        _rawTerminalOutput.value += "\n❌ [ERROR] ${err.message}"
                        _snackbarMessage.emit("خطا در تست کلید API: ${err.message}")
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                _isTestingApi.value = false
                _apiTestResult.value = "اتصال توسط کاربر لغو شد (Cancelled)"
                _rawTerminalOutput.value += "\n⛔ [CANCELLED] درخواست تست API توسط کاربر لغو گردید."
                _snackbarMessage.emit("درخواست تست API لغو شد.")
            }
        }
    }

    fun cancelTestApi() {
        if (testApiJob?.isActive == true) {
            testApiJob?.cancel()
            _isTestingApi.value = false
            _apiTestResult.value = "اتصال لغو شد (Cancelled)"
            _rawTerminalOutput.value += "\n⛔ [CANCELLED] عملیات لغو شد."
        }
    }

    fun testFloatingLyricsPopup() {
        val testSong = SongLyric(
            title = "Floating Popup Test",
            artist = "LyricEcho Live Demo",
            language = targetLanguage.value,
            originalLyrics = "This is a test floating line.\nDrag or collapse me anywhere.\nMusixmatch sync is working!",
            translatedLyrics = "این یک خط تست پاپ‌آپ شناور است.\nمی‌توانید مرا به هر نقطه بکشید یا جمع کنید.\nهمگام‌سازی فعال است!",
            linesJson = SongLyric.serializeLines(
                listOf(
                    LyricLine(0, 0L, "This is a test floating lyric line", "این یک خط تست پاپ‌آپ شناور است"),
                    LyricLine(1, 4000L, "Drag or collapse me anywhere on screen", "می‌توانید مرا به هر نقطه بکشید یا جمع کنید"),
                    LyricLine(2, 8000L, "Musixmatch real-time popup is working!", "پاپ‌آپ زنده لیریک با موفقیت کار می‌کند!")
                )
            ),
            durationMs = 120000L,
            cachedTimestamp = System.currentTimeMillis()
        )
        _currentSong.value = testSong
        _currentLines.value = testSong.getLines()
        _isFloatingPopupVisible.value = true
        _isFloatingPopupCollapsed.value = false
        playbackManager.setSongDuration(120000L)
        playbackManager.seekTo(0L)
        playbackManager.play()
        com.example.service.FloatingOverlayController.updateSong(
            song = testSong,
            lines = testSong.getLines(),
            startPositionMs = 0L,
            playing = true
        )
        com.example.service.FloatingOverlayController.showSystemOverlay(getApplication())
        viewModelScope.launch {
            _snackbarMessage.emit("پاپ‌آپ شناور تست باز شد! روی صفحه ظاهر گردید.")
        }
    }

    fun testDatabaseSpeed() {
        viewModelScope.launch {
            val start = System.currentTimeMillis()
            val count = allLibrarySongs.value.size
            val elapsed = System.currentTimeMillis() - start
            _snackbarMessage.emit("تست دیتابیس موفق: خواندن محلی در ${elapsed}ms انجام شد. دیتابیس Room کاملاً سالم است ($count آهنگ ذخیره شده)")
        }
    }

    fun toggleAppMasterEnabled() {
        viewModelScope.launch {
            val next = !isAppMasterEnabled.value
            settingsDataStore.setAppMasterEnabled(next)
            com.example.service.FloatingOverlayController.setMasterEnabled(next)
            if (!next) {
                com.example.service.FloatingOverlayController.hideSystemOverlay(getApplication())
                _isFloatingPopupVisible.value = false
                playbackManager.pause()
                _snackbarMessage.emit("سیستم LyricEcho غیرفعال (خاموش) شد")
            } else {
                _snackbarMessage.emit("سیستم LyricEcho فعال گردید")
            }
        }
    }

    fun setAppTheme(theme: com.example.ui.theme.AppThemeMode) {
        viewModelScope.launch {
            settingsDataStore.setAppThemeMode(theme)
            com.example.service.FloatingOverlayController.setAppThemeMode(theme)
        }
    }

    fun setPopupScale(scale: String) {
        viewModelScope.launch {
            settingsDataStore.setPopupScale(scale)
            com.example.service.FloatingOverlayController.setPopupScale(scale)
        }
    }

    fun setPopupTapToCollapse(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setPopupTapToCollapseEnabled(enabled)
            com.example.service.FloatingOverlayController.setTapToCollapseEnabled(enabled)
        }
    }

    override fun onCleared() {
        super.onCleared()
        playbackManager.release()
    }
}
