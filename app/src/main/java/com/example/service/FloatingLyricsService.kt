package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.WindowManager
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
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.example.MainActivity
import com.example.data.model.LyricLine
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogTag
import com.example.ui.theme.ElectricCyan
import com.example.ui.theme.NeonViolet
import com.example.ui.theme.ObsidianBorder
import com.example.ui.theme.SunsetRose
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

class FloatingServiceLifecycleOwner : LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val store = ViewModelStore()
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore: ViewModelStore get() = store
    override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

    init {
        savedStateRegistryController.performRestore(Bundle())
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        store.clear()
    }
}

class FloatingLyricsService : Service() {

    private var windowManager: WindowManager? = null
    private var composeView: ComposeView? = null
    private var lifecycleOwner: FloatingServiceLifecycleOwner? = null
    private var windowLayoutParams: WindowManager.LayoutParams? = null

    companion object {
        const val ACTION_SHOW = "com.example.service.ACTION_SHOW"
        const val ACTION_HIDE = "com.example.service.ACTION_HIDE"
        private const val NOTIFICATION_ID = 5050
        private const val CHANNEL_ID = "lyricecho_system_overlay"
    }

    private var spotifyReceiver: com.example.receiver.SpotifyBroadcastReceiver? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        setupFloatingWindow()
        spotifyReceiver = com.example.receiver.SpotifyBroadcastReceiver.register(this)
        MusicNotificationListener.rebindService(this)
    }

    private fun startForegroundNotification() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Lyrics Overlay",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps bilingual floating lyrics visible on top of other apps"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("LyricEcho AI: پاپ‌آپ شناور فعال است")
            .setContentText("متن و ترجمه هم‌زمان روی صفحه در حال نمایش است")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun setupFloatingWindow() {
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        lifecycleOwner = FloatingServiceLifecycleOwner()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        windowLayoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 160
        }

        val owner = lifecycleOwner ?: return
        composeView = ComposeView(this).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindowOrReleasedFromPool)
            setViewTreeLifecycleOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)

            setContent {
                SystemOverlayContent(
                    onDrag = { dx, dy ->
                        windowLayoutParams?.let { params ->
                            params.x += dx.toInt()
                            params.y += dy.toInt()
                            try {
                                windowManager?.updateViewLayout(composeView, params)
                            } catch (_: Exception) {}
                        }
                    },
                    onClose = {
                        FloatingOverlayController.onUserDismissOverlay()
                        stopSelf()
                    },
                    onOpenApp = {
                        val intent = Intent(this@FloatingLyricsService, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        }
                        startActivity(intent)
                    }
                )
            }
        }

        try {
            windowManager?.addView(composeView, windowLayoutParams)
            FloatingOverlayController.onServiceShown()
            AppLogger.s(
                LogTag.OVERLAY_POPUP,
                "پنجره شناور سراسری روی صفحه سوار شد (System Overlay Window Added to WindowManager)",
                "Coordinates: (${windowLayoutParams?.x}, ${windowLayoutParams?.y})"
            )
        } catch (e: Exception) {
            AppLogger.e(LogTag.OVERLAY_POPUP, "خطا در اضافه کردن پنجره به WindowManager: ${e.message}")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_HIDE) {
            stopSelf()
            return START_NOT_STICKY
        }
        FloatingOverlayController.onServiceShown()
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        FloatingOverlayController.onServiceDestroyed()
        spotifyReceiver?.let {
            try {
                unregisterReceiver(it)
            } catch (_: Exception) {}
            spotifyReceiver = null
        }
        composeView?.let { view ->
            try {
                windowManager?.removeView(view)
                AppLogger.i(LogTag.OVERLAY_POPUP, "پنجره شناور سیستم با موفقیت حذف گردید")
            } catch (_: Exception) {}
        }
        lifecycleOwner?.destroy()
        composeView = null
        lifecycleOwner = null
    }
}

@Composable
fun SystemOverlayContent(
    onDrag: (dx: Float, dy: Float) -> Unit,
    onClose: () -> Unit,
    onOpenApp: () -> Unit
) {
    val song by FloatingOverlayController.currentSong.collectAsStateWithLifecycle()
    val lines by FloatingOverlayController.currentLines.collectAsStateWithLifecycle()
    val isCollapsed by FloatingOverlayController.isCollapsed.collectAsStateWithLifecycle()
    val currentPositionMs by FloatingOverlayController.currentPositionMs.collectAsStateWithLifecycle()
    val durationMs by FloatingOverlayController.durationMs.collectAsStateWithLifecycle()
    val isPlaying by FloatingOverlayController.isPlaying.collectAsStateWithLifecycle()
    val syncOffsetMs by FloatingOverlayController.syncOffsetMs.collectAsStateWithLifecycle()
    val activeModelName by FloatingOverlayController.activeModelName.collectAsStateWithLifecycle()
    val themeMode by FloatingOverlayController.appThemeMode.collectAsStateWithLifecycle()
    val popupScale by FloatingOverlayController.popupScale.collectAsStateWithLifecycle()
    val tapToCollapseEnabled by FloatingOverlayController.tapToCollapseEnabled.collectAsStateWithLifecycle()

    val (cardMinWidth, cardMaxWidth, lyricFontSize, transFontSize) = when (popupScale) {
        "Small" -> listOf(240.dp, 280.dp, 12.sp, 11.sp)
        "Large" -> listOf(330.dp, 390.dp, 16.sp, 15.sp)
        "Extra Large", "XL" -> listOf(370.dp, 440.dp, 18.sp, 17.sp)
        else -> listOf(280.dp, 340.dp, 14.sp, 13.sp)
    }

    val primaryColor = themeMode.primaryColor
    val secondaryColor = themeMode.secondaryColor

    val effectivePos = (currentPositionMs + syncOffsetMs).coerceAtLeast(0L)

    val activeLineIndex = run {
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= effectivePos) {
                active = i
            } else {
                break
            }
        }
        if (active == -1 && lines.isNotEmpty()) 0 else active
    }

    val activeLine = lines.getOrNull(activeLineIndex)
    val nextLine = lines.getOrNull(activeLineIndex + 1)

    AnimatedVisibility(
        visible = isCollapsed,
        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        // Floating Draggable Bubble Head showing App Logo
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(primaryColor, Color(0xFF10141E))
                    )
                )
                .border(2.dp, secondaryColor, CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                }
                .clickable { FloatingOverlayController.toggleCollapse() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = "Expand Floating Lyrics",
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
    }

    AnimatedVisibility(
        visible = !isCollapsed,
        enter = scaleIn(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
        exit = scaleOut() + fadeOut()
    ) {
        // Full Draggable Floating Card - Draggable from ANY edge or surface
        Surface(
            modifier = Modifier
                .widthIn(min = cardMinWidth as androidx.compose.ui.unit.Dp, max = cardMaxWidth as androidx.compose.ui.unit.Dp)
                .clip(RoundedCornerShape(20.dp))
                .border(1.5.dp, Brush.horizontalGradient(listOf(primaryColor, secondaryColor)), RoundedCornerShape(20.dp))
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        onDrag(dragAmount.x, dragAmount.y)
                    }
                },
            color = Color(0xF00D111A),
            shadowElevation = 14.dp
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(primaryColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = song?.title ?: "در انتظار پخش اسپاتیفای...",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${song?.artist ?: "Spotify Live Sync"} • $activeModelName",
                                color = secondaryColor,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Re-fetch Studio Synced Lyrics
                        IconButton(
                            onClick = { FloatingOverlayController.onRefreshCallback?.invoke() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Refresh, contentDescription = "دریافت مجدد لیریک استودیویی", tint = secondaryColor, modifier = Modifier.size(16.dp))
                        }
                        // Collapse to Bubble
                        IconButton(
                            onClick = { FloatingOverlayController.toggleCollapse() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Remove, contentDescription = "Collapse", tint = TextMuted)
                        }
                        // Open Full App
                        IconButton(
                            onClick = onOpenApp,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Fullscreen, contentDescription = "Full App", tint = Color.White)
                        }
                        // Close
                        IconButton(
                            onClick = onClose,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = SunsetRose)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Time Display Row (زمان رد شده | زمان کل | زمان باقی‌مانده)
                val effectiveDuration = if (durationMs > 0) durationMs else 180000L
                val remainingMs = (effectiveDuration - effectivePos).coerceAtLeast(0L)

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
                            text = formatTime(effectivePos),
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

                // Progress Indicator
                val progress = if (effectiveDuration > 0) (effectivePos.toFloat() / effectiveDuration.toFloat()).coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = ElectricCyan,
                    trackColor = Color(0xFF1E2536)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Active Lyrics Container (با قابلیت کوچک شدن با لمس روی مرکز در صورت فعال بودن تنظیمات)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF131824))
                        .border(1.dp, ObsidianBorder, RoundedCornerShape(12.dp))
                        .clickable(enabled = tapToCollapseEnabled) {
                            FloatingOverlayController.toggleCollapse()
                        }
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (activeLine != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = activeLine.original,
                                color = Color.White,
                                fontSize = lyricFontSize as androidx.compose.ui.unit.TextUnit,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                lineHeight = (lyricFontSize.value + 4).sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            val hasTranslation = activeLine.translated.isNotBlank() && activeLine.translated.trim() != activeLine.original.trim()
                            if (hasTranslation) {
                                Text(
                                    text = activeLine.translated,
                                    color = secondaryColor,
                                    fontSize = transFontSize as androidx.compose.ui.unit.TextUnit,
                                    fontWeight = FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    style = androidx.compose.ui.text.TextStyle(
                                        textDirection = androidx.compose.ui.text.style.TextDirection.Rtl
                                    ),
                                    lineHeight = (transFontSize.value + 4).sp
                                )
                            } else {
                                Text(
                                    text = "⏳ در حال ترجمه با $activeModelName...",
                                    color = SunsetRose.copy(alpha = 0.85f),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (nextLine != null) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = nextLine.original,
                                    color = TextMuted.copy(alpha = 0.5f),
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "🎵 در حال گوش دادن به آهنگ...",
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Playback & Precision Sync Adjustment Controls Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F131D))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Mini Play/Pause & Seek Controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                val newPos = (effectivePos - 5000L).coerceAtLeast(0L)
                                FloatingOverlayController.onSeekCallback?.invoke(newPos)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastRewind,
                                contentDescription = "-5s",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { FloatingOverlayController.onPlayPauseCallback?.invoke() },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (isPlaying) ElectricCyan else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        IconButton(
                            onClick = {
                                val newPos = (effectivePos + 5000L).coerceAtMost(effectiveDuration)
                                FloatingOverlayController.onSeekCallback?.invoke(newPos)
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FastForward,
                                contentDescription = "+5s",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    // Precision Sync Adjustment Controls (-0.5s / +0.5s / Reset)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (syncOffsetMs == 0L) "سینک: 0.0s" else "سینک: ${if (syncOffsetMs > 0) "+" else ""}${syncOffsetMs / 1000.0}s",
                            color = if (syncOffsetMs == 0L) TextSecondary else NeonViolet,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )

                        // Advance lyrics (-0.5s)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E2536))
                                .clickable { FloatingOverlayController.adjustSyncOffset(-500L) }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("-0.5s", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        // Delay lyrics (+0.5s)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF1E2536))
                                .clickable { FloatingOverlayController.adjustSyncOffset(500L) }
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text("+0.5s", color = ElectricCyan, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }

                        if (syncOffsetMs != 0L) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SunsetRose.copy(alpha = 0.2f))
                                    .clickable { FloatingOverlayController.resetSyncOffset() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text("↺", color = SunsetRose, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
