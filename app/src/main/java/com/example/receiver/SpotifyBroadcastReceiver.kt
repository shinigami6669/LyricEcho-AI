package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpotifyTrackEvent(
    val track: String,
    val artist: String,
    val isPlaying: Boolean,
    val positionMs: Long,
    val durationMs: Long
)

object SpotifyMediaManager {
    private const val TAG = "SpotifyMediaManager"

    private val _lastTrackEvent = MutableStateFlow<SpotifyTrackEvent?>(null)
    val lastTrackEvent: StateFlow<SpotifyTrackEvent?> = _lastTrackEvent.asStateFlow()

    private val _trackChangeEvent = MutableSharedFlow<SpotifyTrackEvent>(extraBufferCapacity = 5)
    val trackChangeEvent: SharedFlow<SpotifyTrackEvent> = _trackChangeEvent.asSharedFlow()

    fun onTrackReceived(track: String, artist: String, isPlaying: Boolean, pos: Long, duration: Long) {
        val event = SpotifyTrackEvent(
            track = track.trim(),
            artist = artist.trim(),
            isPlaying = isPlaying,
            positionMs = pos,
            durationMs = if (duration > 0) duration else 180000L
        )
        _lastTrackEvent.value = event
        _trackChangeEvent.tryEmit(event)
        Log.d(TAG, "Spotify media detected: ${event.track} by ${event.artist} (Playing=$isPlaying)")

        com.example.diagnostics.AppLogger.s(
            com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
            "رویداد آهنگ جدید دریافت شد: \"${event.track}\" - ${event.artist}",
            "Playing: $isPlaying | Position: ${pos}ms | Duration: ${event.durationMs}ms"
        )
    }

    /**
     * Broadcast simulation helper for browser / testing emulator without native Spotify installed.
     */
    fun simulateSpotifyBroadcast(context: Context, track: String, artist: String, durationMs: Long = 180000L) {
        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
            "ارسال برادکست شبیه‌سازی‌شده اسپاتیفای: \"$track\" - $artist",
            "Intent Action: com.spotify.music.metadatachanged | Duration: ${durationMs}ms"
        )
        val intent = Intent("com.spotify.music.metadatachanged").apply {
            putExtra("track", track)
            putExtra("artist", artist)
            putExtra("album", "Spotify Hit Album")
            putExtra("playing", true)
            putExtra("playbackPosition", 0)
            putExtra("length", durationMs.toInt())
        }
        context.sendBroadcast(intent)
    }
}

class SpotifyBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // Bundle extras string dump for detailed diagnostics
        val extras = intent.extras
        val bundleDump = if (extras != null) {
            extras.keySet().joinToString(separator = ", ") { key ->
                "$key=${extras.get(key)}"
            }
        } else {
            "No extras"
        }

        com.example.diagnostics.AppLogger.d(
            com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
            "اینتنت برادکست دریافت شد: $action",
            "Extras: [$bundleDump]"
        )

        // Support Spotify and standard Android media broadcast intents
        val isMetadata = action == "com.spotify.music.metadatachanged" ||
                action == "com.android.music.metadatachanged" ||
                action.endsWith(".metadatachanged")

        val isPlayback = action == "com.spotify.music.playbackstatechanged" ||
                action == "com.android.music.playstatechanged" ||
                action.endsWith(".playbackstatechanged")

        if (!com.example.service.FloatingOverlayController.isMasterEnabled.value) return

        if (isMetadata || isPlayback) {
            val lastEvent = SpotifyMediaManager.lastTrackEvent.value
            val rawTrack = intent.getStringExtra("track")
                ?: intent.getStringExtra("song")
                ?: intent.getStringExtra("track_name")
                ?: (if (isPlayback) lastEvent?.track else null)
            val track = rawTrack?.trim().orEmpty()

            val rawArtist = intent.getStringExtra("artist")
                ?: intent.getStringExtra("artist_name")
                ?: (if (isPlayback) lastEvent?.artist else null)
            val artist = rawArtist?.trim()?.takeIf { it.isNotEmpty() } ?: "Unknown Artist"

            val isPlaying = when {
                intent.hasExtra("playing") -> intent.getBooleanExtra("playing", true)
                intent.hasExtra("playState") -> intent.getBooleanExtra("playState", true)
                intent.hasExtra("state") -> intent.getStringExtra("state")?.equals("play", true) ?: true
                else -> true
            }

            val position = (extras?.get("playbackPosition") as? Number)?.toLong()
                ?: (extras?.get("position") as? Number)?.toLong()
                ?: intent.getIntExtra("playbackPosition", 0).toLong()

            val duration = (extras?.get("length") as? Number)?.toLong()
                ?: (extras?.get("duration") as? Number)?.toLong()
                ?: intent.getIntExtra("length", 0).toLong()
                .let { if (it > 0L) it else (lastEvent?.durationMs ?: 180000L) }

            if (track.isNotBlank()) {
                SpotifyMediaManager.onTrackReceived(track, artist, isPlaying, position, duration)
            } else {
                com.example.diagnostics.AppLogger.w(
                    com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                    "برادکست متادیتا فاقد عنوان آهنگ بود (Empty track name)",
                    "Action: $action, Extras: $bundleDump"
                )
            }
        }
    }

    companion object {
        fun register(context: Context): SpotifyBroadcastReceiver {
            val receiver = SpotifyBroadcastReceiver()
            val filter = IntentFilter().apply {
                addAction("com.spotify.music.metadatachanged")
                addAction("com.spotify.music.playbackstatechanged")
                addAction("com.spotify.music.queuechanged")
                addAction("com.android.music.metadatachanged")
                addAction("com.android.music.playstatechanged")
                addAction("com.android.music.playbackcomplete")
            }

            ContextCompat.registerReceiver(
                context,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED
            )

            com.example.diagnostics.AppLogger.i(
                com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                "گیرنده پویای اسپاتیفای در کانتکست ثبت گردید (Dynamic Receiver Registered)",
                "Listening for: com.spotify.music.metadatachanged, com.spotify.music.playbackstatechanged"
            )
            return receiver
        }
    }
}
