package com.example.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

enum class LogLevel(val label: String, val badgeColorHex: Long) {
    VERBOSE("VERB", 0xFF6B7280),
    DEBUG("DEBUG", 0xFF38BDF8),
    INFO("INFO", 0xFF60A5FA),
    SUCCESS("OK", 0xFF34D399),
    WARN("WARN", 0xFFFBBF24),
    ERROR("ERROR", 0xFFF87171)
}

enum class LogTag(val display: String) {
    SPOTIFY_RECEIVER("Spotify Bcast"),
    NOTIFICATION_LISTENER("Notif Listener"),
    GEMINI_AI("Gemini AI"),
    OVERLAY_POPUP("Floating Popup"),
    PERMISSION_CHECK("Permissions"),
    DATABASE_ROOM("Room SQLite"),
    PLAYBACK_ENGINE("Audio Player"),
    SYSTEM("System")
}

data class LogEntry(
    val id: Long,
    val timestamp: Long,
    val timeFormatted: String,
    val level: LogLevel,
    val tag: LogTag,
    val message: String,
    val details: String? = null
)

object AppLogger {
    private val counter = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())
    private const val MAX_LOGS = 600

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    init {
        i(
            LogTag.SYSTEM,
            "سیستم لاگ‌برداری پیشرفته LyricEcho AI فعال گردید (Logging initialized)",
            "Ready to capture Spotify broadcasts, notification listeners, Gemini AI inference, and overlay states."
        )
    }

    fun log(level: LogLevel, tag: LogTag, message: String, details: String? = null) {
        val now = System.currentTimeMillis()
        val entry = LogEntry(
            id = counter.getAndIncrement(),
            timestamp = now,
            timeFormatted = timeFormat.format(Date(now)),
            level = level,
            tag = tag,
            message = message,
            details = details
        )

        // Android Logcat output
        when (level) {
            LogLevel.VERBOSE -> Log.v("LyricEcho-${tag.display}", "$message ${details ?: ""}")
            LogLevel.DEBUG -> Log.d("LyricEcho-${tag.display}", "$message ${details ?: ""}")
            LogLevel.INFO -> Log.i("LyricEcho-${tag.display}", "$message ${details ?: ""}")
            LogLevel.SUCCESS -> Log.i("LyricEcho-${tag.display}", "[SUCCESS] $message ${details ?: ""}")
            LogLevel.WARN -> Log.w("LyricEcho-${tag.display}", "$message ${details ?: ""}")
            LogLevel.ERROR -> Log.e("LyricEcho-${tag.display}", "$message ${details ?: ""}")
        }

        synchronized(this) {
            val current = _logs.value
            val updated = if (current.size >= MAX_LOGS) {
                current.drop(current.size - MAX_LOGS + 1) + entry
            } else {
                current + entry
            }
            _logs.value = updated
        }
    }

    fun v(tag: LogTag, message: String, details: String? = null) = log(LogLevel.VERBOSE, tag, message, details)
    fun d(tag: LogTag, message: String, details: String? = null) = log(LogLevel.DEBUG, tag, message, details)
    fun i(tag: LogTag, message: String, details: String? = null) = log(LogLevel.INFO, tag, message, details)
    fun s(tag: LogTag, message: String, details: String? = null) = log(LogLevel.SUCCESS, tag, message, details)
    fun w(tag: LogTag, message: String, details: String? = null) = log(LogLevel.WARN, tag, message, details)
    fun e(tag: LogTag, message: String, details: String? = null) = log(LogLevel.ERROR, tag, message, details)

    fun clear() {
        synchronized(this) {
            _logs.value = emptyList()
        }
        s(LogTag.SYSTEM, "کنسول لاگ‌ها پاک‌سازی شد (Log console cleared)")
    }

    fun copyToClipboard(context: Context): String {
        val sb = StringBuilder()
        sb.append("=== گزارش تفصیلی لاگ‌های سیستم LyricEcho AI ===\n")
        sb.append("تاریخ: ").append(SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())).append("\n\n")
        _logs.value.forEach { entry ->
            sb.append("[${entry.timeFormatted}] [${entry.level.name}] [${entry.tag.display}]: ${entry.message}\n")
            if (!entry.details.isNullOrBlank()) {
                sb.append("   -> جزئیات: ${entry.details}\n")
            }
        }
        val text = sb.toString()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("LyricEcho Diagnostics Log", text)
        clipboard.setPrimaryClip(clip)
        return text
    }
}
