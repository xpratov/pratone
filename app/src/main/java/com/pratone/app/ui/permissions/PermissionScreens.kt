package com.pratone.app.ui.permissions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratone.app.ui.theme.Accent

/**
 * Shown before any runtime permission dialog fires (spec section 27: "do not request
 * permissions before the user understands why they are needed"). Media access is asked for
 * first since it's required for the app to do anything at all; microphone access is explained
 * and requested separately, and only once the user chooses to enable voice control — it is
 * never bundled into first launch.
 */
@Composable
fun MediaPermissionRationale(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.LibraryMusic, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 16.dp))
        Text("Find your music", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            "Pratone needs access to your device's music library to show your songs, albums, and artists. " +
                "Nothing is uploaded anywhere — your library stays on your device.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onContinue, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Accent)) {
            Text("Continue")
        }
    }
}

@Composable
fun MicrophonePermissionRationale(onContinue: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(Icons.Filled.Mic, contentDescription = null, tint = Accent, modifier = Modifier.padding(bottom = 16.dp))
        Text("Voice control", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Text(
            "To control playback by saying \"Hey Pratone\", the app needs microphone access. " +
                "Voice recognition happens on your device where possible. While voice control is on, " +
                "a persistent notification shows that the microphone is active — this is an Android " +
                "requirement and can't be hidden. You can turn voice control off anytime in Settings.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 16.dp),
        )
        Button(onClick = onContinue, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Accent)) {
            Text("Enable voice control")
        }
        TextButton(onClick = onSkip) { Text("Not now") }
    }
}
