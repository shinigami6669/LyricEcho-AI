package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.example.data.model.LyricLine
import com.example.data.model.SongLyric
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogTag
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object FloatingOverlayController {
    private val _currentSong = MutableStateFlow<SongLyric?>(null)
    val currentSong: StateFlow<SongLyric?> = _currentSong.asStateFlow()

    private val _currentLines = MutableStateFlow<List<LyricLine>>(emptyList())
    val currentLines: StateFlow<List<LyricLine>> = _currentLines.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(180000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _syncOffsetMs = MutableStateFlow(0L)
    val syncOffsetMs: StateFlow<Long> = _syncOffsetMs.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isCollapsed = MutableStateFlow(false)
    val isCollapsed: StateFlow<Boolean> = _isCollapsed.asStateFlow()

    private val _activeModelName = MutableStateFlow("gemini-2.5-flash")
    val activeModelName: StateFlow<String> = _activeModelName.asStateFlow()

    private val _isMasterEnabled = MutableStateFlow(true)
    val isMasterEnabled: StateFlow<Boolean> = _isMasterEnabled.asStateFlow()

    private val _popupScale = MutableStateFlow("Medium")
    val popupScale: StateFlow<String> = _popupScale.asStateFlow()

    private val _tapToCollapseEnabled = MutableStateFlow(true)
    val tapToCollapseEnabled: StateFlow<Boolean> = _tapToCollapseEnabled.asStateFlow()

    private val _appThemeMode = MutableStateFlow(com.example.ui.theme.AppThemeMode.NEON_OBSIDIAN)
    val appThemeMode: StateFlow<com.example.ui.theme.AppThemeMode> = _appThemeMode.asStateFlow()

    fun setMasterEnabled(enabled: Boolean) {
        _isMasterEnabled.value = enabled
    }

    fun setPopupScale(scale: String) {
        _popupScale.value = scale
    }

    fun setTapToCollapseEnabled(enabled: Boolean) {
        _tapToCollapseEnabled.value = enabled
    }

    fun setAppThemeMode(mode: com.example.ui.theme.AppThemeMode) {
        _appThemeMode.value = mode
    }

    fun setActiveModel(model: String) {
        if (model.isNotBlank()) {
            _activeModelName.value = model
        }
    }

    var onPlayPauseCallback: (() -> Unit)? = null
    var onSeekCallback: ((Long) -> Unit)? = null
    var onRefreshCallback: (() -> Unit)? = null
    var onUserDismissOverlayCallback: (() -> Unit)? = null

    fun onServiceShown() {
        _isOverlayActive.value = true
    }

    fun onServiceDestroyed() {
        _isOverlayActive.value = false
    }

    fun onUserDismissOverlay() {
        _isOverlayActive.value = false
        onUserDismissOverlayCallback?.invoke()
    }

    fun updateSong(
        song: SongLyric,
        lines: List<LyricLine>,
        startPositionMs: Long = 0L,
        playing: Boolean = true,
        explicitDurationMs: Long? = null
    ) {
        _currentSong.value = song
        _currentLines.value = lines
        val dur = explicitDurationMs ?: (if (song.durationMs > 0) song.durationMs else 180000L)
        _durationMs.value = if (dur > 0) dur else 180000L
        _currentPositionMs.value = startPositionMs
        _isPlaying.value = playing
    }

    fun updatePosition(posMs: Long, isPlaying: Boolean? = null, durationMs: Long? = null) {
        _currentPositionMs.value = posMs
        if (isPlaying != null) {
            _isPlaying.value = isPlaying
        }
        if (durationMs != null && durationMs > 0) {
            _durationMs.value = durationMs
        }
    }

    fun adjustSyncOffset(deltaMs: Long) {
        _syncOffsetMs.value += deltaMs
        AppLogger.i(
            LogTag.OVERLAY_POPUP,
            "تغییر تاخیر همگام‌سازی (Sync Offset): ${_syncOffsetMs.value}ms (میزان تغییر: ${deltaMs}ms)",
            "Applied to active lyric tracking to match Spotify playback rhythm."
        )
    }

    fun resetSyncOffset() {
        _syncOffsetMs.value = 0L
    }

    fun toggleCollapse() {
        _isCollapsed.value = !_isCollapsed.value
    }

    fun setCollapsed(collapsed: Boolean) {
        _isCollapsed.value = collapsed
    }

    fun getEffectivePosition(): Long {
        return (_currentPositionMs.value + _syncOffsetMs.value).coerceAtLeast(0L)
    }

    fun getActiveLineIndex(): Int {
        val lines = _currentLines.value
        if (lines.isEmpty()) return -1
        val eff = getEffectivePosition()

        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= eff) {
                active = i
            } else {
                break
            }
        }
        return if (active == -1 && lines.isNotEmpty()) 0 else active
    }

    fun showSystemOverlay(context: Context) {
        if (!_isMasterEnabled.value) {
            AppLogger.w(
                LogTag.OVERLAY_POPUP,
                "سیستم اپلیکیشن خاموش (OFF) است. پاپ‌آپ فعال نشد."
            )
            return
        }
        if (!Settings.canDrawOverlays(context)) {
            AppLogger.w(
                LogTag.OVERLAY_POPUP,
                "دسترسی نمایش روی سایر برنامه‌ها (Overlay) داده نشده است. پاپ‌آپ سراسری نمی‌تواند باز شود."
            )
            return
        }

        try {
            val intent = Intent(context, FloatingLyricsService::class.java).apply {
                action = FloatingLyricsService.ACTION_SHOW
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            _isOverlayActive.value = true
            AppLogger.s(
                LogTag.OVERLAY_POPUP,
                "سرویس پاپ‌آپ شناور سیستم (System Overlay Window) فراخوانی شد",
                "Visible across all applications and home screen."
            )
        } catch (e: Exception) {
            AppLogger.e(LogTag.OVERLAY_POPUP, "خطا در شروع FloatingLyricsService: ${e.message}")
        }
    }

    fun hideSystemOverlay(context: Context) {
        try {
            val intent = Intent(context, FloatingLyricsService::class.java).apply {
                action = FloatingLyricsService.ACTION_HIDE
            }
            context.startService(intent)
            _isOverlayActive.value = false
        } catch (_: Exception) {}
    }
}
