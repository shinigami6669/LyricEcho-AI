package com.example.data.repository

import com.example.data.local.LyricDao
import com.example.data.model.LyricLine
import com.example.data.model.SongLyric
import com.example.data.remote.GeminiLyricsResult
import com.example.data.remote.GeminiLyricsService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FetchLyricResponse(
    val songLyric: SongLyric,
    val isFromCache: Boolean
)

class LyricRepository(
    private val lyricDao: LyricDao,
    private val geminiService: GeminiLyricsService = GeminiLyricsService(),
    private val lrclibService: com.example.data.remote.LrclibService = com.example.data.remote.LrclibService()
) {
    val allSongs: Flow<List<SongLyric>> = lyricDao.getAllSongLyrics()
    val recentSongs: Flow<List<SongLyric>> = lyricDao.getRecentSongLyrics(15)

    init {
        // Clean up previously seeded demo tracks or corrupted cache entries
        CoroutineScope(Dispatchers.IO).launch {
            cleanUpLegacySampleTracks()
        }
    }

    fun searchSongs(query: String): Flow<List<SongLyric>> = lyricDao.searchSongLyrics(query)

    fun getSongById(id: Long): Flow<SongLyric?> = lyricDao.getSongById(id)

    val aiService: GeminiLyricsService = geminiService

    suspend fun getSong(
        title: String,
        artist: String,
        language: String,
        apiSettings: com.example.data.remote.ApiEngineSettings = com.example.data.remote.ApiEngineSettings(),
        userCustomKey: String? = null,
        forceRefresh: Boolean = false
    ): Result<FetchLyricResponse> = withContext(Dispatchers.IO) {
        // 1. Check local Room storage first if not force refreshing
        if (!forceRefresh) {
            val cached = lyricDao.findSongLyric(title.trim(), artist.trim(), language.trim())
            if (cached != null) {
                val cachedLines = cached.getLines()
                val hasPersian = cachedLines.any { line -> line.translated.any { c -> c in '\u0600'..'\u06FF' } }
                val hasValidTranslation = cachedLines.isNotEmpty() && cachedLines.any { line ->
                    line.translated.isNotBlank() && line.translated.trim() != line.original.trim()
                } && (language != "Persian" || hasPersian)

                if (!hasValidTranslation) {
                    com.example.diagnostics.AppLogger.w(
                        com.example.diagnostics.LogTag.DATABASE_ROOM,
                        "متن آهنگ «$title» در کش Room فاقد ترجمه به زبان $language است. حذف کش و آغاز ترجمه زنده با مدل «${apiSettings.selectedModel}»"
                    )
                    lyricDao.deleteSongLyric(cached)
                } else if (cached.originalLyrics.contains("flying like a bird", ignoreCase = true) ||
                    cached.originalLyrics.contains("I'm like a bird", ignoreCase = true)
                ) {
                    com.example.diagnostics.AppLogger.w(
                        com.example.diagnostics.LogTag.DATABASE_ROOM,
                        "حذف کش نامعتبر و توهمی آهنگ «$title» جهت استخراج مجدد لیریک رسمی استودیویی"
                    )
                    lyricDao.deleteSongLyric(cached)
                } else {
                    com.example.diagnostics.AppLogger.s(
                        com.example.diagnostics.LogTag.DATABASE_ROOM,
                        "بازیابی با تاخیر 0ms از حافظه محلی Room (Cache Hit): \"$title\" - $artist",
                        "Language: $language | Lines: ${cachedLines.size} | Duration: ${cached.durationMs}ms"
                    )
                    val updated = cached.copy(cachedTimestamp = System.currentTimeMillis())
                    lyricDao.insertSongLyric(updated)
                    return@withContext Result.success(FetchLyricResponse(updated, isFromCache = true))
                }
            } else {
                com.example.diagnostics.AppLogger.d(
                    com.example.diagnostics.LogTag.DATABASE_ROOM,
                    "عدم وجود در حافظه محلی Room (Cache Miss) - شروع جستجو در پایگاه لیریک استودیویی",
                    "Title: $title, Artist: $artist, Language: $language"
                )
            }
        } else {
            val old = lyricDao.findSongLyric(title.trim(), artist.trim(), language.trim())
            if (old != null) {
                lyricDao.deleteSongLyric(old)
                com.example.diagnostics.AppLogger.i(
                    com.example.diagnostics.LogTag.DATABASE_ROOM,
                    "حذف کش قبلی جهت رفرش اجباری با مدل «${apiSettings.selectedModel}»"
                )
            }
        }

        // 2. PRIMARY: Query LRCLIB for 100% verified studio synced lyrics
        val lrcResult = lrclibService.fetchSyncedLyrics(title, artist)
        if (lrcResult.isSuccess) {
            val lrcData = lrcResult.getOrThrow()
            if (lrcData.lines.isNotEmpty()) {
                com.example.diagnostics.AppLogger.s(
                    com.example.diagnostics.LogTag.SPOTIFY_RECEIVER,
                    "متن رسمی و تایم‌استمپ‌های دقیق استودیویی از LRCLIB دریافت شد (${lrcData.lines.size} خط)",
                    "Title: ${lrcData.title} | Duration: ${lrcData.durationMs}ms | Synced: ${lrcData.isSynced}"
                )

                // Translate the authentic lines using AI
                val translatedLinesResult = geminiService.translateAuthenticLyrics(
                    title = lrcData.title,
                    artist = lrcData.artist,
                    lines = lrcData.lines,
                    targetLanguage = language,
                    settings = apiSettings,
                    userCustomKey = userCustomKey
                )
                val finalLines = translatedLinesResult.getOrDefault(lrcData.lines)
                val transRaw = finalLines.joinToString("\n") { it.translated }
                val hasValidTranslations = finalLines.any { line ->
                    line.translated.isNotBlank() &&
                    line.translated.trim() != line.original.trim() &&
                    (language != "Persian" || line.translated.any { c -> c in '\u0600'..'\u06FF' })
                }

                val newEntity = SongLyric(
                    title = lrcData.title,
                    artist = lrcData.artist,
                    language = language,
                    originalLyrics = lrcData.rawPlain,
                    translatedLyrics = transRaw,
                    linesJson = SongLyric.serializeLines(finalLines),
                    albumArtUrl = null,
                    durationMs = lrcData.durationMs,
                    isFavorite = false,
                    cachedTimestamp = System.currentTimeMillis()
                )

                if (hasValidTranslations) {
                    val id = lyricDao.insertSongLyric(newEntity)
                    val savedEntity = newEntity.copy(id = id)
                    return@withContext Result.success(FetchLyricResponse(savedEntity, isFromCache = false))
                } else {
                    com.example.diagnostics.AppLogger.w(
                        com.example.diagnostics.LogTag.DATABASE_ROOM,
                        "ترجمه خطوط برای زبان $language کامل نشد. عدم ذخیره در کش Room تا در تلاش بعدی مجدداً از مدل «${apiSettings.selectedModel}» استعلام شود."
                    )
                    return@withContext Result.success(FetchLyricResponse(newEntity, isFromCache = false))
                }
            }
        }

        // 3. FALLBACK: Fetch via AI generator if song is not in LRCLIB database
        com.example.diagnostics.AppLogger.i(
            com.example.diagnostics.LogTag.GEMINI_AI,
            "ارسال به موتور هوش مصنوعی برای تولید لیریک و همگام‌سازی زمان (پشتیبان LRCLIB)..."
        )
        val aiResult = geminiService.fetchLyricsAndTranslation(
            title = title,
            artist = artist,
            targetLanguage = language,
            settings = apiSettings,
            userCustomKey = userCustomKey
        )

        aiResult.fold(
            onSuccess = { res ->
                val newEntity = SongLyric(
                    title = res.title,
                    artist = res.artist,
                    language = language,
                    originalLyrics = res.rawOriginal,
                    translatedLyrics = res.rawTranslated,
                    linesJson = SongLyric.serializeLines(res.lines),
                    albumArtUrl = null,
                    durationMs = res.durationMs,
                    isFavorite = false,
                    cachedTimestamp = System.currentTimeMillis()
                )
                val id = lyricDao.insertSongLyric(newEntity)
                val savedEntity = newEntity.copy(id = id)
                Result.success(FetchLyricResponse(savedEntity, isFromCache = false))
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }

    suspend fun saveManualLyric(
        title: String,
        artist: String,
        language: String,
        originalLyrics: String,
        translatedLyrics: String
    ): SongLyric = withContext(Dispatchers.IO) {
        val origLines = originalLyrics.lines().filter { it.isNotBlank() }
        val transLines = translatedLyrics.lines().filter { it.isNotBlank() }
        val count = maxOf(origLines.size, transLines.size)
        val stepTime = if (count > 0) 180000L / count else 4000L

        val lyricLines = (0 until count).map { i ->
            LyricLine(
                id = i,
                timeMs = i * stepTime,
                original = origLines.getOrElse(i) { "" },
                translated = transLines.getOrElse(i) { "" }
            )
        }

        val entity = SongLyric(
            title = title,
            artist = artist,
            language = language,
            originalLyrics = originalLyrics,
            translatedLyrics = translatedLyrics,
            linesJson = SongLyric.serializeLines(lyricLines),
            albumArtUrl = null,
            durationMs = 180000L,
            isFavorite = false,
            cachedTimestamp = System.currentTimeMillis()
        )
        val id = lyricDao.insertSongLyric(entity)
        entity.copy(id = id)
    }

    suspend fun toggleFavorite(id: Long, currentFavorite: Boolean) = withContext(Dispatchers.IO) {
        lyricDao.updateFavorite(id, !currentFavorite)
    }

    suspend fun deleteSong(song: SongLyric) = withContext(Dispatchers.IO) {
        lyricDao.deleteSongLyric(song)
    }

    suspend fun deleteSongById(id: Long) = withContext(Dispatchers.IO) {
        lyricDao.deleteSongLyricById(id)
    }

    suspend fun clearCache() = withContext(Dispatchers.IO) {
        lyricDao.clearAllLyrics()
    }

    private suspend fun cleanUpLegacySampleTracks() {
        val sampleTitles = listOf("Baraye", "Viva La Vida", "Someone Like You", "Jane Maryam")
        sampleTitles.forEach { sampleTitle ->
            val samplePersian = lyricDao.findSongLyric(sampleTitle, "Shervin Hajipour", "Persian")
                ?: lyricDao.findSongLyric(sampleTitle, "Coldplay", "Persian")
                ?: lyricDao.findSongLyric(sampleTitle, "Adele", "Persian")
                ?: lyricDao.findSongLyric(sampleTitle, "Mohammad Nouri", "Persian")

            if (samplePersian != null) {
                lyricDao.deleteSongLyric(samplePersian)
            }
        }
        val testPopupSong = lyricDao.findSongLyric("Floating Popup Test", "LyricEcho Live Demo", "Persian")
        if (testPopupSong != null) {
            lyricDao.deleteSongLyric(testPopupSong)
        }
        val chemicalsCached = lyricDao.findSongLyric("Chemicals", "Mud Flow", "Persian")
        if (chemicalsCached != null && chemicalsCached.originalLyrics.contains("bird", ignoreCase = true)) {
            lyricDao.deleteSongLyric(chemicalsCached)
        }
    }
}
