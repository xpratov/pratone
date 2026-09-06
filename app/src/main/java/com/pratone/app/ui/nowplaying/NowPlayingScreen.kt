package com.pratone.app.ui.nowplaying

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.components.AlbumArt
import com.pratone.app.ui.components.ProgressBar
import com.pratone.app.ui.components.SecondaryControls
import com.pratone.app.ui.components.TransportControls
import com.pratone.app.ui.components.VoiceIndicator

/**
 * Dedicated full-screen Now Playing view (spec section 21) — the same underlying state as
 * [com.pratone.app.ui.home.HomeScreen], just laid out for a focused, larger presentation.
 */
@Composable
fun NowPlayingScreen(viewModel: AppViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val playback by viewModel.playbackState.collectAsState()
    val voiceState by viewModel.voiceState.collectAsState()
    val favorites by viewModel.favoriteSongIds.collectAsState()
    val song = playback.currentSong

    Column(modifier = modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onClose) {
                Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "Close now playing")
            }
        }

        Spacer(Modifier.height(12.dp))

        AlbumArt(
            uri = song?.artworkUri,
            modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(24.dp)),
        )

        Spacer(Modifier.height(32.dp))

        Text(
            text = song?.title ?: "Nothing playing",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = listOfNotNull(song?.artist, song?.album).joinToString(" — "),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        )

        Spacer(Modifier.height(28.dp))

        ProgressBar(
            positionMs = viewModel.playerManager.currentPositionMs(),
            durationMs = playback.durationMs,
            onSeek = { viewModel.playerManager.seekTo(it) },
        )

        Spacer(Modifier.height(12.dp))

        TransportControls(
            isPlaying = playback.isPlaying,
            onPlayPause = { viewModel.playerManager.togglePlayPause() },
            onNext = { viewModel.playerManager.next() },
            onPrevious = { viewModel.playerManager.previous() },
            large = true,
        )

        Spacer(Modifier.height(8.dp))

        SecondaryControls(
            shuffleEnabled = playback.shuffleEnabled,
            repeatMode = playback.repeatMode,
            isFavorite = song?.id?.let { it in favorites } ?: false,
            onShuffleToggle = { viewModel.playerManager.setShuffle(!playback.shuffleEnabled) },
            onRepeatCycle = { viewModel.playerManager.cycleRepeatMode() },
            onFavoriteToggle = { song?.let { viewModel.toggleFavorite(it.id) } },
        )

        Spacer(Modifier.height(24.dp))

        VoiceIndicator(state = voiceState, modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp))
    }
}
