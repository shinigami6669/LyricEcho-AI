package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import org.json.JSONArray
import org.json.JSONObject

data class LyricLine(
    val id: Int,
    val timeMs: Long,
    val original: String,
    val translated: String
) {
    fun formatTimestamp(): String {
        val totalSeconds = (timeMs / 1000).toInt()
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d".format(minutes, seconds)
    }

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timeMs", timeMs)
            put("original", original)
            put("translated", translated)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): LyricLine {
            return LyricLine(
                id = json.optInt("id", 0),
                timeMs = json.optLong("timeMs", 0L),
                original = json.optString("original", ""),
                translated = json.optString("translated", "")
            )
        }
    }
}

@Entity(
    tableName = "song_lyrics",
    indices = [
        Index(value = ["title", "artist", "language"], unique = true)
    ]
)
data class SongLyric(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val language: String,
    val originalLyrics: String,
    val translatedLyrics: String,
    val linesJson: String,
    val albumArtUrl: String? = null,
    val durationMs: Long = 180000L,
    val isFavorite: Boolean = false,
    val cachedTimestamp: Long = System.currentTimeMillis()
) {
    fun getLines(): List<LyricLine> {
        if (linesJson.isBlank()) return emptyList()
        val list = mutableListOf<LyricLine>()
        try {
            val jsonArray = JSONArray(linesJson)
            for (i in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(i)
                list.add(LyricLine.fromJson(item))
            }
        } catch (_: Exception) {
            // Fallback: line-by-line split if JSON parsing fails
            val origLines = originalLyrics.lines().filter { it.isNotBlank() }
            val transLines = translatedLyrics.lines().filter { it.isNotBlank() }
            val count = maxOf(origLines.size, transLines.size)
            val stepTime = if (count > 0) durationMs / count else 4000L
            for (i in 0 until count) {
                list.add(
                    LyricLine(
                        id = i,
                        timeMs = i * stepTime,
                        original = origLines.getOrElse(i) { "" },
                        translated = transLines.getOrElse(i) { "" }
                    )
                )
            }
        }
        return list
    }

    companion object {
        fun serializeLines(lines: List<LyricLine>): String {
            val array = JSONArray()
            lines.forEach { array.put(it.toJson()) }
            return array.toString()
        }
    }
}
