package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SongLyric
import kotlinx.coroutines.flow.Flow

@Dao
interface LyricDao {

    @Query("SELECT * FROM song_lyrics ORDER BY cachedTimestamp DESC")
    fun getAllSongLyrics(): Flow<List<SongLyric>>

    @Query("SELECT * FROM song_lyrics ORDER BY cachedTimestamp DESC LIMIT :limit")
    fun getRecentSongLyrics(limit: Int = 15): Flow<List<SongLyric>>

    @Query("""
        SELECT * FROM song_lyrics 
        WHERE title LIKE '%' || :query || '%' 
           OR artist LIKE '%' || :query || '%' 
           OR originalLyrics LIKE '%' || :query || '%' 
           OR translatedLyrics LIKE '%' || :query || '%'
        ORDER BY cachedTimestamp DESC
    """)
    fun searchSongLyrics(query: String): Flow<List<SongLyric>>

    @Query("SELECT * FROM song_lyrics WHERE LOWER(title) = LOWER(:title) AND LOWER(artist) = LOWER(:artist) AND LOWER(language) = LOWER(:language) LIMIT 1")
    suspend fun findSongLyric(title: String, artist: String, language: String): SongLyric?

    @Query("SELECT * FROM song_lyrics WHERE id = :id LIMIT 1")
    fun getSongById(id: Long): Flow<SongLyric?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongLyric(songLyric: SongLyric): Long

    @Delete
    suspend fun deleteSongLyric(songLyric: SongLyric)

    @Query("DELETE FROM song_lyrics WHERE id = :id")
    suspend fun deleteSongLyricById(id: Long)

    @Query("UPDATE song_lyrics SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM song_lyrics")
    suspend fun clearAllLyrics()

    @Query("SELECT COUNT(*) FROM song_lyrics")
    fun getSongCount(): Flow<Int>
}
