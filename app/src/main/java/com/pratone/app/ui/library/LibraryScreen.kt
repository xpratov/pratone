package com.pratone.app.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pratone.app.domain.model.Song
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.theme.Accent

private val tabs = listOf("Songs", "Albums", "Artists", "Playlists")

@Composable
fun LibraryScreen(
    viewModel: AppViewModel,
    onSongSelected: (Song, List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }

    val songs by viewModel.repository.songs.collectAsState()
    val albums by viewModel.repository.albums.collectAsState()
    val artists by viewModel.repository.artists.collectAsState()

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        Text(
            "Library",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(top = 16.dp, bottom = 12.dp),
        )

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search songs, artists, albums") },
            singleLine = true,
            colors = TextFieldDefaults.colors(focusedIndicatorColor = Accent, cursorColor = Accent),
            modifier = Modifier.fillMaxWidth(),
        )

        if (query.isNotBlank()) {
            val results = remember(query, songs) { viewModel.repository.search(query) }
            SongList(results, results, onSongSelected)
            return@Column
        }

        TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.background, contentColor = Accent) {
            tabs.forEachIndexed { index, title ->
                Tab(selected = selectedTab == index, onClick = { selectedTab = index }, text = { Text(title) })
            }
        }

        when (selectedTab) {
            0 -> {
                if (songs.isEmpty()) EmptyLibraryState() else SongList(songs, songs, onSongSelected)
            }
            1 -> LazyColumn {
                items(albums, key = { it.id }) { album ->
                    AlbumRow(album) {
                        val albumSongs = viewModel.repository.songsForAlbum(album.id)
                        albumSongs.firstOrNull()?.let { onSongSelected(it, albumSongs) }
                    }
                }
            }
            2 -> LazyColumn {
                items(artists, key = { it.id }) { artist ->
                    ArtistRow(artist) {
                        val artistSongs = viewModel.repository.songsForArtist(artist.name)
                        artistSongs.firstOrNull()?.let { onSongSelected(it, artistSongs) }
                    }
                }
            }
            3 -> PlaylistsTab(viewModel, onSongSelected)
        }
    }
}

@Composable
private fun SongList(songs: List<Song>, queueContext: List<Song>, onSongSelected: (Song, List<Song>) -> Unit) {
    LazyColumn {
        items(songs, key = { it.id }) { song ->
            SongRow(song = song, onClick = { onSongSelected(song, queueContext) })
        }
    }
}

@Composable
private fun EmptyLibraryState() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("No music found", style = MaterialTheme.typography.titleMedium)
        Text(
            "Add music to your device and refresh the library.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
