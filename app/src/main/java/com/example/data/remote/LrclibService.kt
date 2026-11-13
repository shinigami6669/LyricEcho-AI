package com.example.data.remote

import com.example.data.model.LyricLine
import com.example.diagnostics.AppLogger
import com.example.diagnostics.LogTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LrclibLyricsResult(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val lines: List<LyricLine>,
    val rawPlain: String,
    val isSynced: Boolean
)

class LrclibService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {

    private val lrcPattern = Pattern.compile("^\\[(\\d{1,2}):(\\d{2})(?:[.:](\\d{1,3}))?\\](.*)$")

    suspend fun fetchSyncedLyrics(
        title: String,
        artist: String,
        durationMs: Long = 0L
    ): Result<LrclibLyricsResult> = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = cleanArtistName(artist)

        AppLogger.i(
            LogTag.SPOTIFY_RECEIVER,
            "جستجوی لیریک سینک‌شده استودیویی در LRCLIB برای: \"$cleanTitle\" اثر \"$cleanArtist\"",
            "Raw: title=\"$title\", artist=\"$artist\""
        )

        // 1. Try exact match query
        val exactResult = tryFetchExact(cleanTitle, cleanArtist, durationMs)
        if (exactResult != null && exactResult.isSynced) {
            return@withContext Result.success(exactResult)
        }

        // 2. Try search endpoint if exact failed
        val searchResult = tryFetchSearch(cleanTitle, cleanArtist)
        if (searchResult != null) {
            return@withContext Result.success(searchResult)
        }

        // 3. Fallback: search with raw uncleaned title if cleaned failed
        if (cleanTitle != title.trim()) {
            val rawSearchResult = tryFetchSearch(title.trim(), artist.trim())
            if (rawSearchResult != null) {
                return@withContext Result.success(rawSearchResult)
            }
        }

        AppLogger.d(
            LogTag.SPOTIFY_RECEIVER,
            "آهنگ \"$cleanTitle\" در دیتابیس LRCLIB موجود نیست. ارجاع به موتور هوش مصنوعی..."
        )
        Result.failure(Exception("Lyrics not found on LRCLIB for \"$cleanTitle\" by \"$cleanArtist\""))
    }

    private fun tryFetchExact(
        title: String,
        artist: String,
        durationMs: Long
    ): LrclibLyricsResult? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            var url = "https://lrclib.net/api/get?track_name=$encTitle&artist_name=$encArtist"
            if (durationMs > 0) {
                url += "&duration=${durationMs / 1000}"
            }

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LyricEchoAI/1.0 (Android; contact=support@lyricecho.app)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            parseLrclibJson(body, title, artist)
        } catch (e: Exception) {
            AppLogger.d(LogTag.SPOTIFY_RECEIVER, "LRCLIB exact match lookup note: ${e.message}")
            null
        }
    }

    private fun tryFetchSearch(
        title: String,
        artist: String
    ): LrclibLyricsResult? {
        return try {
            val encTitle = URLEncoder.encode(title, "UTF-8")
            val encArtist = URLEncoder.encode(artist, "UTF-8")
            val url = "https://lrclib.net/api/search?track_name=$encTitle&artist_name=$encArtist"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "LyricEchoAI/1.0 (Android; contact=support@lyricecho.app)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) return null

            val body = response.body?.string() ?: return null
            val array = JSONArray(body)
            if (array.length() == 0) return null

            // Prioritize results with syncedLyrics
            var bestResult: LrclibLyricsResult? = null
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val synced = item.optString("syncedLyrics", "")
                val parsed = parseLrclibJsonObject(item, title, artist)
                if (parsed != null) {
                    if (parsed.isSynced) {
                        return parsed // Instant return on synced match!
                    }
                    if (bestResult == null) {
                        bestResult = parsed
                    }
                }
            }
            bestResult
        } catch (e: Exception) {
            AppLogger.d(LogTag.SPOTIFY_RECEIVER, "LRCLIB search error: ${e.message}")
            null
        }
    }

    private fun parseLrclibJson(jsonStr: String, fallbackTitle: String, fallbackArtist: String): LrclibLyricsResult? {
        return try {
            val obj = JSONObject(jsonStr)
            parseLrclibJsonObject(obj, fallbackTitle, fallbackArtist)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseLrclibJsonObject(obj: JSONObject, fallbackTitle: String, fallbackArtist: String): LrclibLyricsResult? {
        val title = obj.optString("trackName", fallbackTitle).ifBlank { fallbackTitle }
        val artist = obj.optString("artistName", fallbackArtist).ifBlank { fallbackArtist }
        val durationSec = obj.optDouble("duration", 180.0)
        val durationMs = (durationSec * 1000).toLong().coerceAtLeast(60000L)
        val syncedLyrics = obj.optString("syncedLyrics", "")
        val plainLyrics = obj.optString("plainLyrics", "")

        if (syncedLyrics.isNotBlank()) {
            val lines = parseLrcString(syncedLyrics)
            if (lines.isNotEmpty()) {
                return LrclibLyricsResult(
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    lines = lines,
                    rawPlain = plainLyrics.ifBlank { lines.joinToString("\n") { it.original } },
                    isSynced = true
                )
            }
        }

        if (plainLyrics.isNotBlank()) {
            val lines = plainLyrics.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() }
            if (lines.isNotEmpty()) {
                val step = durationMs / lines.size.coerceAtLeast(1)
                val lyricLines = lines.mapIndexed { idx, text ->
                    LyricLine(
                        id = idx,
                        timeMs = idx * step,
                        original = text,
                        translated = ""
                    )
                }
                return LrclibLyricsResult(
                    title = title,
                    artist = artist,
                    durationMs = durationMs,
                    lines = lyricLines,
                    rawPlain = plainLyrics,
                    isSynced = false
                )
            }
        }

        return null
    }

    fun parseLrcString(lrcContent: String): List<LyricLine> {
        val result = mutableListOf<LyricLine>()
        var idCounter = 0

        lrcContent.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                val matcher = lrcPattern.matcher(trimmed)
                if (matcher.matches()) {
                    val minutes = matcher.group(1)?.toLongOrNull() ?: 0L
                    val seconds = matcher.group(2)?.toLongOrNull() ?: 0L
                    val fracStr = matcher.group(3) ?: "0"
                    val fracMs = when (fracStr.length) {
                        1 -> fracStr.toLong() * 100L
                        2 -> fracStr.toLong() * 10L
                        3 -> fracStr.toLong()
                        else -> fracStr.take(3).toLongOrNull() ?: 0L
                    }
                    val timeMs = (minutes * 60L + seconds) * 1000L + fracMs
                    val text = matcher.group(4)?.trim() ?: ""

                    // Include valid lyric line (skip purely blank timing lines unless between verses)
                    if (text.isNotBlank()) {
                        result.add(
                            LyricLine(
                                id = idCounter++,
                                timeMs = timeMs,
                                original = text,
                                translated = ""
                            )
                        )
                    }
                }
            }
        }

        return result.sortedBy { it.timeMs }
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex("(?i)\\s*-\\s*(remastered|bonus track|live|radio edit|deluxe|version|mono|single).*"), "")
            .replace(Regex("(?i)\\s*\\((remastered|bonus track|live|radio edit|deluxe|version|mono|single|feat\\..*|with .*)\\)"), "")
            .replace(Regex("(?i)\\[(remastered|bonus track|live|radio edit|deluxe|version|mono|single|feat\\..*|with .*)\\]"), "")
            .trim()
    }

    private fun cleanArtistName(artist: String): String {
        return artist
            .split(Regex("(?i)\\s*(feat\\.|ft\\.|,|&|with)\\s*"))
            .firstOrNull()?.trim() ?: artist.trim()
    }
}
