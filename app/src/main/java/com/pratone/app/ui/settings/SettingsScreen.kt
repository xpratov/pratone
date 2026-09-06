package com.pratone.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pratone.app.data.local.MediaPermissions
import com.pratone.app.domain.model.RepeatMode
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.theme.Accent
import com.pratone.app.voice.AudioEffectsHelper

/**
 * Settings screen (spec section 25). Voice-control state here drives [com.pratone.app.voice.VoiceListeningService]
 * directly through the shared [AppViewModel] — there is no separate "settings state" that could
 * drift out of sync with what's actually running.
 */
@Composable
fun SettingsScreen(viewModel: AppViewModel, onNeedsMicRationale: () -> Unit, modifier: Modifier = Modifier) {
    val voiceEnabled by viewModel.voiceEnabled.collectAsState()
    val hasRecordAudio by viewModel.hasRecordAudioPermission.collectAsState()
    val playback by viewModel.playbackState.collectAsState()
    var accessKeyField by remember { mutableStateOf("") }

    LazyColumn(modifier = modifier.fillMaxSize().padding(horizontal = 20.dp)) {
        item {
            Text("Settings", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(vertical = 16.dp))
            SectionHeader("Voice")
        }

        item {
            SettingRow(
                title = "Voice control",
                subtitle = if (hasRecordAudio) "Listens in the background for \"Hey Pratone\"" else "Microphone permission required",
            ) {
                Switch(
                    checked = voiceEnabled && hasRecordAudio,
                    onCheckedChange = { checked ->
                        if (checked && !hasRecordAudio) {
                            onNeedsMicRationale()
                        } else {
                            viewModel.setVoiceEnabled(checked)
                        }
                    },
                    enabled = true,
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                )
            }
        }

        item {
            Text(
                "Engine: ${viewModel.activeVoiceEngineName}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(vertical = 6.dp),
            )
        }

        item {
            Text("Wake phrase: \"Hey Pratone\"", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Custom wake phrases require a trained Porcupine model — see README.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        item {
            Column(Modifier.padding(vertical = 12.dp)) {
                Text("Porcupine access key (optional)", style = MaterialTheme.typography.bodyMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = accessKeyField,
                        onValueChange = { accessKeyField = it },
                        singleLine = true,
                        placeholder = { Text("Paste key from console.picovoice.ai") },
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { viewModel.setPorcupineAccessKey(accessKeyField) }) { Text("Save") }
                }
            }
        }

        item {
            Text(
                "Microphone status: ${if (hasRecordAudio) "granted" else "not granted"} · " +
                    "Echo cancellation: ${if (AudioEffectsHelper.isAecAvailable()) "available" else "unavailable on this device"}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 16.dp),
            )
        }

        item { Divider(); SectionHeader("Playback") }

        item {
            SettingRow(title = "Shuffle", subtitle = null) {
                Switch(
                    checked = playback.shuffleEnabled,
                    onCheckedChange = { viewModel.playerManager.setShuffle(it) },
                    colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                )
            }
        }
        item {
            SettingRow(
                title = "Repeat",
                subtitle = when (playback.repeatMode) {
                    RepeatMode.OFF -> "Off"
                    RepeatMode.ALL -> "All"
                    RepeatMode.ONE -> "One"
                },
            ) {
                TextButton(onClick = { viewModel.playerManager.cycleRepeatMode() }) { Text("Change") }
            }
        }

        item { Divider(); SectionHeader("Library") }

        item {
            SettingRow(title = "Rescan music", subtitle = "Pick up new or changed files") {
                TextButton(onClick = { viewModel.rescanLibrary() }) { Text("Rescan") }
            }
        }
        item {
            SettingRow(
                title = "Media permission",
                subtitle = if (MediaPermissions.hasReadAudio(androidx.compose.ui.platform.LocalContext.current)) "Granted" else "Not granted",
            ) {}
        }

        item { Divider(); SectionHeader("About") }
        item {
            Text("Pratone — a minimalist, voice-controlled music player.", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Music playback and browsing work fully offline. Voice recognition uses on-device " +
                    "processing where the configured engine supports it; see README for details.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(top = 6.dp, bottom = 32.dp),
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String?, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.secondary)
            }
        }
        trailing()
    }
}
