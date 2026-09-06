package com.pratone.app.ui.components

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.pratone.app.domain.model.RepeatMode
import com.pratone.app.ui.theme.Accent
import com.pratone.app.ui.theme.SurfaceElevated
import java.util.concurrent.TimeUnit

@Composable
fun AlbumArt(uri: Uri?, modifier: Modifier = Modifier) {
    AsyncImage(
        model = uri,
        contentDescription = "Album artwork",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .padding(4.dp),
    )
}

@Composable
fun TransportControls(
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    modifier: Modifier = Modifier,
    large: Boolean = false,
) {
    val buttonSize = if (large) 40.dp else 32.dp
    val playSize = if (large) 56.dp else 44.dp
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(buttonSize))
        }
        IconButton(onClick = onPlayPause) {
            Icon(
                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = Accent,
                modifier = Modifier.size(playSize),
            )
        }
        IconButton(onClick = onNext) {
            Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(buttonSize))
        }
    }
}

@Composable
fun SecondaryControls(
    shuffleEnabled: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    onShuffleToggle: () -> Unit,
    onRepeatCycle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        IconButton(onClick = onShuffleToggle) {
            Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle", tint = if (shuffleEnabled) Accent else MaterialTheme.colorScheme.secondary)
        }
        IconButton(onClick = onRepeatCycle) {
            Icon(
                if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                contentDescription = "Repeat",
                tint = if (repeatMode != RepeatMode.OFF) Accent else MaterialTheme.colorScheme.secondary,
            )
        }
        IconButton(onClick = onFavoriteToggle) {
            Icon(
                if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                contentDescription = "Favorite",
                tint = if (isFavorite) Accent else MaterialTheme.colorScheme.secondary,
            )
        }
    }
}

@Composable
fun ProgressBar(
    positionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f,
            onValueChange = { fraction -> onSeek((fraction * durationMs).toLong()) },
            colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = SurfaceElevated),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(formatDuration(positionMs), style = MaterialTheme.typography.labelSmall)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelSmall)
        }
    }
}

fun formatDuration(ms: Long): String {
    val totalSeconds = TimeUnit.MILLISECONDS.toSeconds(ms.coerceAtLeast(0))
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
