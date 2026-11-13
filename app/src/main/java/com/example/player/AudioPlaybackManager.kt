package com.example.player

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Pure virtual playback timing engine.
 * Responsibilities:
 * - Tracks accurate playback progress (milliseconds) to drive karaoke highlighting and smooth scrolling.
 * - Does NOT output any audio through speakers/headphones (guaranteeing 100% silence so Spotify's audio is untouched).
 */
class AudioPlaybackManager(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(180000L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _playbackSpeed = MutableStateFlow(1.0f)
    val playbackSpeed: StateFlow<Float> = _playbackSpeed.asStateFlow()

    private var tickerJob: Job? = null

    fun setSongDuration(duration: Long) {
        _durationMs.value = if (duration > 0) duration else 180000L
        if (_currentPositionMs.value > _durationMs.value) {
            _currentPositionMs.value = 0L
        }
    }

    fun play() {
        if (_isPlaying.value) return
        _isPlaying.value = true
        startTicker()
    }

    fun pause() {
        _isPlaying.value = false
        tickerJob?.cancel()
    }

    fun togglePlayPause() {
        if (_isPlaying.value) pause() else play()
    }

    fun seekTo(positionMs: Long) {
        _currentPositionMs.value = positionMs.coerceIn(0L, _durationMs.value)
    }

    fun setSpeed(speed: Float) {
        _playbackSpeed.value = speed.coerceIn(0.25f, 3.0f)
    }

    private fun startTicker() {
        tickerJob?.cancel()
        tickerJob = scope.launch(Dispatchers.Default) {
            var lastTime = System.currentTimeMillis()
            while (isActive && _isPlaying.value) {
                delay(33) // ~30 fps smooth position ticker
                val now = System.currentTimeMillis()
                val delta = (now - lastTime) * _playbackSpeed.value
                lastTime = now

                val nextPos = _currentPositionMs.value + delta.toLong()
                if (nextPos >= _durationMs.value) {
                    _currentPositionMs.value = _durationMs.value
                    pause()
                    _currentPositionMs.value = 0L
                    break
                } else {
                    _currentPositionMs.value = nextPos
                }
            }
        }
    }

    fun release() {
        pause()
    }
}
