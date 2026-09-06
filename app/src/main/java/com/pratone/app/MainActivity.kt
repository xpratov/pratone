package com.pratone.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pratone.app.data.local.MediaPermissions
import com.pratone.app.ui.AppViewModel
import com.pratone.app.ui.navigation.PratoneNavGraph
import com.pratone.app.ui.permissions.MediaPermissionRationale
import com.pratone.app.ui.permissions.MicrophonePermissionRationale
import com.pratone.app.ui.theme.PratoneTheme

/**
 * Single-activity host. Permission flow per spec section 27: media permission is requested up
 * front (the app is useless without it); microphone permission is only requested if/when the
 * user opts into voice control from [com.pratone.app.ui.permissions.MicrophonePermissionRationale]
 * or the Settings screen — never bundled into first launch.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: AppViewModel by viewModels()

    private val requestMediaPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionsUpdated()
    }
    private val requestMicPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        viewModel.onPermissionsUpdated()
        if (granted) viewModel.setVoiceEnabled(true)
    }
    private val requestNotificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !MediaPermissions.hasNotifications(this)) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        setContent {
            PratoneTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppRoot(
                        viewModel = viewModel,
                        onRequestMediaPermission = { requestMediaPermission.launch(MediaPermissions.readAudioPermission) },
                        onRequestMicPermission = { requestMicPermission.launch(MediaPermissions.recordAudioPermission) },
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onPermissionsUpdated()
    }
}

@Composable
private fun AppRoot(
    viewModel: AppViewModel,
    onRequestMediaPermission: () -> Unit,
    onRequestMicPermission: () -> Unit,
) {
    val hasMedia by viewModel.hasReadAudioPermission.collectAsState()
    var showMicRationale by remember { mutableStateOf(false) }

    when {
        !hasMedia -> MediaPermissionRationale(onContinue = onRequestMediaPermission)
        showMicRationale -> MicrophonePermissionRationale(
            onContinue = { showMicRationale = false; onRequestMicPermission() },
            onSkip = { showMicRationale = false },
        )
        else -> PratoneNavGraph(viewModel = viewModel, onNeedsMicRationale = { showMicRationale = true })
    }
}
