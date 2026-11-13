package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogTag
import com.example.receiver.SpotifyMediaManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class MusicNotificationListener : NotificationListenerService() {

    private var sessionsListener: MediaSessionManager.OnActiveSessionsChangedListener? = null

    override fun onListenerConnected() {
        super.onListenerConnected()
        activeInstance = this
        _isListenerConnected.value = true
        AppLogger.s(
            LogTag.NOTIFICATION_LISTENER,
            "سرویس شنونده اعلان‌ها با موفقیت متصل شد (NotificationListenerService connected)",
            "System notification hook active. Scanning active notifications and media sessions for Spotify..."
        )
        rescanActiveNotificationsAndSessions()
        registerMediaSessionsListener()
    }

    fun rescanActiveNotificationsAndSessions() {
        if (!FloatingOverlayController.isMasterEnabled.value) return
        // 1. Scan active notifications
        try {
            val active = activeNotifications
            if (active != null && active.isNotEmpty()) {
                AppLogger.d(
                    LogTag.NOTIFICATION_LISTENER,
                    "اسکن اعلان‌های فعال: ${active.size} اعلان فعال در سیستم بررسی می‌شوند"
                )
                for (sbn in active) {
                    onNotificationPosted(sbn)
                }
            }
        } catch (e: Exception) {
            AppLogger.d(LogTag.NOTIFICATION_LISTENER, "Active notifications scan note: ${e.message}")
        }

        // 2. Scan active system media sessions (Hardware MediaSessionManager)
        scanActiveMediaSessions()
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        if (activeInstance === this) activeInstance = null
        _isListenerConnected.value = false
        AppLogger.w(
            LogTag.NOTIFICATION_LISTENER,
            "سرویس شنونده اعلان‌ها قطع ارتباط شد (NotificationListenerService disconnected)",
            "System unhooked the listener. Rebinding will be attempted automatically."
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        if (activeInstance === this) activeInstance = null
        _isListenerConnected.value = false
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager
            sessionsListener?.let { msm?.removeOnActiveSessionsChangedListener(it) }
        } catch (_: Exception) {}
        sessionsListener = null
    }

    private fun registerMediaSessionsListener() {
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val component = ComponentName(this, MusicNotificationListener::class.java)
            if (sessionsListener == null) {
                sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
                    controllers?.forEach { handleMediaController(it) }
                }
                msm.addOnActiveSessionsChangedListener(sessionsListener!!, component)
                AppLogger.i(
                    LogTag.NOTIFICATION_LISTENER,
                    "شنونده مستقیم جلسات صوتی سیستم (ActiveSessionsChangedListener) فعال شد"
                )
            }
        } catch (e: Exception) {
            AppLogger.d(LogTag.NOTIFICATION_LISTENER, "ثبت شنونده مدیاسشن: ${e.message}")
        }
    }

    private fun scanActiveMediaSessions() {
        try {
            val msm = getSystemService(Context.MEDIA_SESSION_SERVICE) as? MediaSessionManager ?: return
            val component = ComponentName(this, MusicNotificationListener::class.java)
            val controllers = msm.getActiveSessions(component)
            if (!controllers.isNullOrEmpty()) {
                AppLogger.d(
                    LogTag.NOTIFICATION_LISTENER,
                    "اسکن جلسات صوتی سیستم: ${controllers.size} جلسه پخش فعال یافت شد"
                )
                for (controller in controllers) {
                    handleMediaController(controller)
                }
            }
        } catch (e: Exception) {
            AppLogger.d(LogTag.NOTIFICATION_LISTENER, "خطا در بررسی سشن‌های مدیا: ${e.message}")
        }
    }

    private fun handleMediaController(controller: MediaController) {
        if (!FloatingOverlayController.isMasterEnabled.value) return
        try {
            val pkg = controller.packageName ?: ""
            val isSpotify = pkg.contains("spotify", ignoreCase = true)
            val isMusic = isSpotify || pkg.contains("music", ignoreCase = true) || pkg.contains("audio", ignoreCase = true)
            if (!isMusic) return

            val state = controller.playbackState
            val isPlaying = state?.state == PlaybackState.STATE_PLAYING
            val meta = controller.metadata
            val track = meta?.getString(MediaMetadata.METADATA_KEY_TITLE)
            val artist = meta?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: "Unknown Artist"
            val dur = meta?.getLong(MediaMetadata.METADATA_KEY_DURATION) ?: 180000L
            val pos = state?.position ?: 0L

            if (!track.isNullOrBlank() && isPlaying) {
                AppLogger.s(
                    LogTag.NOTIFICATION_LISTENER,
                    "شناسایی مستقیم موزیک از MediaSession سیستم: \"$track\" اثر $artist ($pkg)",
                    "Position: ${pos}ms | Duration: ${dur}ms"
                )
                SpotifyMediaManager.onTrackReceived(
                    track = track,
                    artist = artist,
                    isPlaying = isPlaying,
                    pos = pos,
                    duration = if (dur > 0) dur else 180000L
                )
                FloatingOverlayController.updatePosition(pos, isPlaying, durationMs = dur)
                FloatingOverlayController.showSystemOverlay(this)
            }
        } catch (e: Exception) {
            AppLogger.d(LogTag.NOTIFICATION_LISTENER, "خطا در استخراج اطلاعات سشن: ${e.message}")
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (!FloatingOverlayController.isMasterEnabled.value) return
        if (sbn == null) return
        val pkg = sbn.packageName ?: return

        val notif = sbn.notification ?: return
        val extras = notif.extras ?: return
        val category = notif.category
        val hasMediaSession = extras.containsKey(Notification.EXTRA_MEDIA_SESSION)

        // Focus on Spotify, media sessions, and common Android audio media players
        val isSpotify = pkg.equals("com.spotify.music", ignoreCase = true) || pkg.contains("spotify", ignoreCase = true)
        val isOtherMusic = pkg.contains("music", ignoreCase = true) ||
                pkg.contains("audio", ignoreCase = true) ||
                pkg.contains("sound", ignoreCase = true) ||
                pkg.contains("deezer", ignoreCase = true) ||
                pkg.contains("radio", ignoreCase = true) ||
                pkg.contains("podcast", ignoreCase = true)

        val isMediaPlayback = isSpotify || isOtherMusic || hasMediaSession || category == Notification.CATEGORY_TRANSPORT
        if (!isMediaPlayback) return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim()
        val artist = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
            ?: extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()?.trim()
            ?: "Unknown Artist"

        // Read exact live playback position and duration from MediaSession if available
        var actualPosition = 0L
        var actualDuration = 180000L
        var isPlayingState = true

        try {
            val sessionToken = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)
            } else {
                @Suppress("DEPRECATION")
                extras.getParcelable(Notification.EXTRA_MEDIA_SESSION) as? MediaSession.Token
            }

            if (sessionToken != null) {
                val mediaController = MediaController(this, sessionToken)
                val state = mediaController.playbackState
                if (state != null) {
                    actualPosition = state.position
                    isPlayingState = state.state == PlaybackState.STATE_PLAYING
                }
                val meta = mediaController.metadata
                if (meta != null) {
                    val dur = meta.getLong(MediaMetadata.METADATA_KEY_DURATION)
                    if (dur > 0) actualDuration = dur
                }
                AppLogger.d(
                    LogTag.NOTIFICATION_LISTENER,
                    "موقعیت زنده دقیق از MediaSession اسپاتیفای خوانده شد: ${actualPosition}ms / ${actualDuration}ms (Playing=$isPlayingState)"
                )
            }
        } catch (e: Exception) {
            AppLogger.d(LogTag.NOTIFICATION_LISTENER, "MediaController read note: ${e.message}")
        }

        AppLogger.d(
            LogTag.NOTIFICATION_LISTENER,
            "اعلان مدیا دریافت شد از $pkg: \"$title\" - \"$artist\"",
            "Package: $pkg | Category: $category | MediaSession: $hasMediaSession | NotificationID: ${sbn.id}"
        )

        // Ignore empty title or app notifications like "Downloading"
        if (!title.isNullOrBlank() && !title.contains("download", ignoreCase = true) && !title.contains("sync", ignoreCase = true)) {
            AppLogger.s(
                LogTag.NOTIFICATION_LISTENER,
                "موزیک فعال با موفقیت از اعلان استخراج شد: \"$title\" اثر $artist (موقعیت: ${actualPosition}ms)",
                "Triggering LyricEcho auto-sync pipeline for $pkg..."
            )
            SpotifyMediaManager.onTrackReceived(
                track = title,
                artist = artist,
                isPlaying = isPlayingState,
                pos = actualPosition,
                duration = actualDuration
            )
            FloatingOverlayController.updatePosition(actualPosition, isPlayingState, durationMs = actualDuration)
            if (isPlayingState) {
                FloatingOverlayController.showSystemOverlay(this)
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        val pkg = sbn?.packageName ?: return
        if (pkg.contains("spotify", ignoreCase = true)) {
            AppLogger.i(
                LogTag.NOTIFICATION_LISTENER,
                "اعلان اسپاتیفای از نوار وضعیت حذف شد (Notification dismissed)",
                "Playback was likely paused or stopped by the user in Spotify."
            )
        }
    }

    companion object {
        @Volatile
        private var activeInstance: MusicNotificationListener? = null

        private val _isListenerConnected = MutableStateFlow(false)
        val isListenerConnected: StateFlow<Boolean> = _isListenerConnected.asStateFlow()

        fun isNotificationListenerGranted(context: Context): Boolean {
            val packageName = context.packageName
            val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
            return flat != null && flat.contains(packageName)
        }

        /**
         * Automatically ensures the notification listener service is bound and active.
         * If the listener is already connected, directly rescans active notifications and media sessions
         * without disabling the component (which would otherwise unhook an active listener on app reopen).
         * Only performs requestRebind + PackageManager component toggle when the service is disconnected.
         */
        fun rebindService(context: Context, forceToggle: Boolean = false) {
            if (!isNotificationListenerGranted(context)) return

            val currentInstance = activeInstance
            if (!forceToggle && _isListenerConnected.value && currentInstance != null) {
                AppLogger.d(
                    LogTag.NOTIFICATION_LISTENER,
                    "شنونده اعلان‌ها متصل است — اجرای اسکن فوری جلسات صوتی فعال (Instant MediaSession Rescan)"
                )
                currentInstance.rescanActiveNotificationsAndSessions()
                return
            }

            val component = ComponentName(context, MusicNotificationListener::class.java)

            // 1. Android 7.0+ requestRebind
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    requestRebind(component)
                    AppLogger.d(LogTag.NOTIFICATION_LISTENER, "ارسال درخواست اتصال مجدد (requestRebind) به اندروید")
                } catch (e: Exception) {
                    AppLogger.d(LogTag.NOTIFICATION_LISTENER, "درخواست requestRebind: ${e.message}")
                }
            }

            // 2. PackageManager component toggle trick (only when disconnected or forced)
            try {
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                    PackageManager.DONT_KILL_APP
                )
                pm.setComponentEnabledSetting(
                    component,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
                AppLogger.s(
                    LogTag.NOTIFICATION_LISTENER,
                    "ری‌کانکت خودکار شنونده اعلان‌ها انجام شد (Forced re-bind via PackageManager component refresh)"
                )
            } catch (e: Exception) {
                AppLogger.w(LogTag.NOTIFICATION_LISTENER, "خطا در ری‌کانکت کامپوننت: ${e.message}")
            }
        }

        fun openNotificationListenerSettings(context: Context) {
            try {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                AppLogger.i(
                    LogTag.PERMISSION_CHECK,
                    "صفحه تنظیمات دسترسی به اعلان‌های اندروید باز شد (Opened Notification Access Settings)"
                )
            } catch (e: Exception) {
                AppLogger.e(
                    LogTag.PERMISSION_CHECK,
                    "عدم موفقیت در باز کردن تنظیمات نوتیفیکیشن: ${e.message}"
                )
            }
        }
    }
}
