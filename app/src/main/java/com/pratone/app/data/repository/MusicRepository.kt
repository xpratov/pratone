package com.pratone.app.data.repository

import android.content.Context
import com.pratone.app.data.local.AppDatabase
import com.pratone.app.data.local.FavoriteEntity
import com.pratone.app.data.local.MediaStoreScanner
import com.pratone.app.data.local.RecentlyPlayedEntity
import com.pratone.app.domain.model.Album
import com.pratone.app.domain.model.Artist
import com.pratone.app.domain.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Single source of truth for "what music exists on this device". Backed by [MediaStoreScanner]
 * for the library itself and Room ([AppDatabase]) for app-only metadata (favorites, playlists,
 * recently played) that MediaStore has no concept of.
 */
class MusicRepository(context: Context) {

    private val appContext = context.applicationContext
    private val scanner = MediaStoreScanner(appContext)
    private val db = AppDatabase.get(appContext)

    private val _songs = MutableStateFlow<List<Song>>(emptyList())
    val songs: StateFlow<List<Song>> = _songs.asStateFlow()

    private val _albums = MutableStateFlow<List<Album>>(emptyList())
    val albums: StateFlow<List<Album>> = _albums.asStateFlow()

    private val _artists = MutableStateFlow<List<Artist>>(emptyList())
    val artists: StateFlow<List<Artist>> = _artists.asStateFlow()

    suspend fun rescan() {
        _songs.value = scanner.scanSongs()
        _albums.value = scanner.scanAlbums()
        _artists.value = scanner.scanArtists()
    }

    fun songsForAlbum(albumId: Long): List<Song> = _songs.value.filter { it.albumId == albumId }
        .sortedBy { it.trackNumber }

    fun songsForArtist(artistName: String): List<Song> = _songs.value.filter { it.artist == artistName }

    fun search(query: String): List<Song> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return _songs.value.filter {
            it.title.lowercase().contains(q) || it.artist.lowercase().contains(q) || it.album.lowercase().contains(q)
        }
    }

    fun observeFavorites() = db.favoriteDao().observeFavorites().map { list -> list.map { it.songId }.toSet() }

    suspend fun toggleFavorite(songId: Long) {
        if (db.favoriteDao().isFavorite(songId)) {
            db.favoriteDao().remove(songId)
        } else {
            db.favoriteDao().add(FavoriteEntity(songId))
        }
    }

    suspend fun favoriteSongs(): List<Song> {
        val ids = db.favoriteDao().observeFavorites().first().map { it.songId }.toSet()
        return _songs.value.filter { it.id in ids }
    }

    suspend fun markPlayed(songId: Long) {
        db.recentlyPlayedDao().markPlayed(RecentlyPlayedEntity(songId))
    }

    fun observeRecentlyPlayed() = db.recentlyPlayedDao().observeRecent()

    fun songById(id: Long): Song? = _songs.value.firstOrNull { it.id == id }

    fun playlistRepository() = PlaylistRepository(db, this)
}
