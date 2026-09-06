package com.pratone.app.data.local

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

/** Centralizes the version-dependent permission strings so no other file has to branch on SDK_INT. */
object MediaPermissions {

    /** The permission needed to read the local audio library on this device's API level. */
    val readAudioPermission: String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    const val recordAudioPermission: String = Manifest.permission.RECORD_AUDIO

    val notificationsPermission: String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.POST_NOTIFICATIONS
        } else null

    fun hasReadAudio(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, readAudioPermission) == PackageManager.PERMISSION_GRANTED

    fun hasRecordAudio(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, recordAudioPermission) == PackageManager.PERMISSION_GRANTED

    fun hasNotifications(context: Context): Boolean =
        notificationsPermission?.let {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        } ?: true
}
