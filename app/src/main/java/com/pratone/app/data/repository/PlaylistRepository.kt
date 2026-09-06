package com.pratone.app.data.repository

import com.pratone.app.data.local.AppDatabase
import com.pratone.app.data.local.PlaylistEntity
import com.pratone.app.data.local.PlaylistSongCrossRef
import com.pratone.app.domain.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class PlaylistRepository(private val db: AppDatabase, private val musicRepository: MusicRepository) {

    fun observePlaylists(): Flow<List<PlaylistEntity>> = db.playlistDao().observePlaylists()

    suspend fun createPlaylist(name: String): Long = db.playlistDao().insert(PlaylistEntity(name = name))

    suspend fun renamePlaylist(id: Long, name: String) = db.playlistDao().rename(id, name)

    suspend fun deletePlaylist(id: Long) = db.playlistDao().delete(id)

    suspend fun addSong(playlistId: Long, songId: Long) {
        val position = db.playlistDao().nextPosition(playlistId)
        db.playlistDao().addSong(PlaylistSongCrossRef(playlistId, songId, position))
    }

    suspend fun removeSong(playlistId: Long, songId: Long) = db.playlistDao().removeSong(playlistId, songId)

    fun observeSongsForPlaylist(playlistId: Long): Flow<List<Song>> =
        db.playlistDao().observeSongsForPlaylist(playlistId).map { refs ->
            refs.mapNotNull { musicRepository.songById(it.songId) }
        }

    suspend fun songsForPlaylistOnce(playlistId: Long): List<Song> =
        db.playlistDao().observeSongsForPlaylist(playlistId).first().mapNotNull { musicRepository.songById(it.songId) }
}
