package com.pratone.app.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pratone.app.data.local.PlaylistEntity
import com.pratone.app.data.repository.PlaylistRepository
import com.pratone.app.domain.model.Song
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.theme.Accent
import kotlinx.coroutines.launch

@Composable
fun PlaylistsTab(viewModel: AppViewModel, onSongSelected: (Song, List<Song>) -> Unit) {
    val playlistRepository = remember { viewModel.repository.playlistRepository() }
    val playlists by playlistRepository.observePlaylists().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }
    var openedPlaylist by remember { mutableStateOf<PlaylistEntity?>(null) }
    val scope = rememberCoroutineScope()

    val opened = openedPlaylist
    if (opened != null) {
        PlaylistDetail(
            playlist = opened,
            playlistRepository = playlistRepository,
            onBack = { openedPlaylist = null },
            onSongSelected = onSongSelected,
        )
        return
    }

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Your playlists", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = "Create playlist", tint = Accent)
            }
        }

        LazyColumn {
            items(playlists, key = { it.id }) { playlist ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        playlist.name,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f).clickableSafe { openedPlaylist = playlist },
                    )
                    IconButton(onClick = { scope.launch { playlistRepository.deletePlaylist(playlist.id) } }) {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete playlist")
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                scope.launch { playlistRepository.createPlaylist(name) }
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun PlaylistDetail(
    playlist: PlaylistEntity,
    playlistRepository: PlaylistRepository,
    onBack: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
) {
    val songs by playlistRepository.observeSongsForPlaylist(playlist.id).collectAsState(initial = emptyList())

    Column(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(playlist.name, style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = onBack) { Text("Back") }
        }
        if (songs.isEmpty()) {
            Text(
                "No songs yet. Add some from the Songs tab.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 24.dp),
            )
        } else {
            LazyColumn {
                items(songs, key = { it.id }) { song ->
                    SongRow(song = song, onClick = { onSongSelected(song, songs) })
                }
            }
        }
    }
}

@Composable
private fun CreatePlaylistDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New playlist") },
        text = {
            OutlinedTextField(value = name, onValueChange = { name = it }, singleLine = true, placeholder = { Text("Playlist name") })
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank()) onCreate(name) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

// Small helper so the row Text can be tappable without importing clickable at every call site.
private fun Modifier.clickableSafe(onClick: () -> Unit): Modifier = this.clickable(onClick = onClick)
