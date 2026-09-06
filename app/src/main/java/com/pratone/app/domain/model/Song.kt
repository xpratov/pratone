package com.pratone.app.domain.model

import android.net.Uri

/**
 * A song as discovered from MediaStore. [id] is the MediaStore _ID for the audio row and is
 * used to build stable content:// Uris for both playback ([contentUri]) and artwork
 * ([artworkUri]) — we never copy audio bytes ourselves.
 */
data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUri: Uri,
    val artworkUri: Uri?,
    val trackNumber: Int = 0,
)

data class Album(
    val id: Long,
    val title: String,
    val artist: String,
    val artworkUri: Uri?,
    val songCount: Int,
)

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
)
