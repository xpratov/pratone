package com.pratone.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtMs: Long = System.currentTimeMillis(),
)

/** Join row. [songId] is the MediaStore _ID for the song — the source of truth stays MediaStore. */
@Entity(tableName = "playlist_songs", primaryKeys = ["playlistId", "songId"])
data class PlaylistSongCrossRef(
    val playlistId: Long,
    val songId: Long,
    val position: Int,
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val songId: Long,
    val addedAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "recently_played")
data class RecentlyPlayedEntity(
    @PrimaryKey val songId: Long,
    val playedAtMs: Long = System.currentTimeMillis(),
)
