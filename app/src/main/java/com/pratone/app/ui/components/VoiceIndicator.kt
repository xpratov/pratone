package com.pratone.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pratone.app.ui.theme.Accent
import com.pratone.app.voice.VoiceState
import com.pratone.app.voice.statusText

/**
 * The mic indicator: a soft pulsing ring while actively listening (wake word or command), a
 * plain still icon when idle, and a status line beneath it. Animation is a single infinite
 * scale/alpha transition — intentionally subtle and cheap to keep battery impact low while it's
 * on screen (the animation itself has no bearing on the actual background listening cost, which
 * lives in [com.pratone.app.voice.VoiceController]).
 */
@Composable
fun VoiceIndicator(state: VoiceState, modifier: Modifier = Modifier) {
    val isActive = state is VoiceState.ListeningForWakeWord ||
        state is VoiceState.WakeWordDetected ||
        state is VoiceState.ListeningForCommand

    val transition = rememberInfiniteTransition(label = "voice-pulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (isActive) 1.18f else 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse-scale",
    )
    val ringAlpha by transition.animateFloat(
        initialValue = 0.15f,
        targetValue = if (isActive) 0.35f else 0.15f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulse-alpha",
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(72.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse; alpha = ringAlpha }
                    .background(Accent, CircleShape),
            )
            Icon(
                imageVector = if (state is VoiceState.Idle) Icons.Filled.MicOff else Icons.Filled.Mic,
                contentDescription = "Voice control status",
                tint = if (isActive) Accent else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(32.dp),
            )
        }
        Text(
            text = state.statusText(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 12.dp),
        )
    }
}
