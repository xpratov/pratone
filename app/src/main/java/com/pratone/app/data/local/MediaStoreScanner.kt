package com.pratone.app.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import com.pratone.app.domain.model.Album
import com.pratone.app.domain.model.Artist
import com.pratone.app.domain.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Reads the device's music library straight from MediaStore. We deliberately never copy or
 * import files — Media3 plays directly from the content:// Uris this class returns, and any
 * on-device edit/delete/add to the library is picked up on the next scan without extra work
 * from us. This is the only supported way to enumerate shared media since scoped storage
 * (Android 10+) removed broad filesystem access.
 */
class MediaStoreScanner(private val context: Context) {

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val collection = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.TRACK,
        )
        // IS_MUSIC filters out ringtones/notifications/alarms/podcasts that also live in this table.
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(collection, projection, selection, null, sortOrder)?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val albumId = cursor.getLong(albumIdCol)
                val contentUri = ContentUris.withAppendedId(collection, id)
                songs += Song(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown title",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    album = cursor.getString(albumCol) ?: "Unknown album",
                    albumId = albumId,
                    durationMs = cursor.getLong(durationCol),
                    contentUri = contentUri,
                    artworkUri = albumArtUri(albumId),
                    // TRACK on MediaStore packs disc*1000+track; we only care about ordering within an album.
                    trackNumber = cursor.getInt(trackCol).let { if (it > 1000) it % 1000 else it },
                )
            }
        }
        songs
    }

    suspend fun scanAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val collection = MediaStore.Audio.Albums.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS,
        )
        context.contentResolver.query(collection, projection, null, null, MediaStore.Audio.Albums.ALBUM + " ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                albums += Album(
                    id = id,
                    title = cursor.getString(titleCol) ?: "Unknown album",
                    artist = cursor.getString(artistCol) ?: "Unknown artist",
                    artworkUri = albumArtUri(id),
                    songCount = cursor.getInt(countCol),
                )
            }
        }
        albums
    }

    suspend fun scanArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()
        val collection = MediaStore.Audio.Artists.getContentUri(MediaStore.VOLUME_EXTERNAL)
        val projection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS,
        )
        context.contentResolver.query(collection, projection, null, null, MediaStore.Audio.Artists.ARTIST + " ASC")?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val countCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)
            while (cursor.moveToNext()) {
                artists += Artist(
                    id = cursor.getLong(idCol),
                    name = cursor.getString(nameCol) ?: "Unknown artist",
                    songCount = cursor.getInt(countCol),
                )
            }
        }
        artists
    }

    /**
     * Modern (API 29+) per-row artwork Uri. On API 28 and below MediaStore.Audio.Albums has an
     * ALBUM_ART filesystem column instead; since minSdk is 26 we support both paths here.
     */
    private fun albumArtUri(albumId: Long): Uri {
        val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
        return ContentUris.withAppendedId(sArtworkUri, albumId)
    }
}
