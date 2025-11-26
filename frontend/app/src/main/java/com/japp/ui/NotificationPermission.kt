package com.japp.ui

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun RequestNotificationPermission(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    val context = LocalContext.current
    val permissionState = rememberPermissionState(
        permission = Manifest.permission.POST_NOTIFICATIONS
    ) { granted ->
        onPermissionResult(granted)
        if (!granted) {
            Toast.makeText(
                context,
                "Notifications disabled. You can enable them in Settings.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    LaunchedEffect(permissionState.status.isGranted) {
        if (permissionState.status.isGranted) {
            onPermissionResult(true)
            return@LaunchedEffect
        }
    }

    if (!permissionState.status.isGranted) {
        if (permissionState.status.shouldShowRationale) {
            RationaleDialog(
                onConfirm = { permissionState.launchPermissionRequest() },
                onDismiss = { onPermissionResult(false) }
            )
        } else {
            LaunchedEffect(Unit) {
                permissionState.launchPermissionRequest()
            }
        }
    }
}

@Composable
private fun RationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Enable Notifications") },
        text = {
            Text(
                "Japp needs notification permission to alert you about new expenses, " +
                        "settlements and messages from your groups."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Allow")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Not Now")
            }
        }
    )
}

@Composable
fun NotificationPermissionHandler(
    onPermissionResult: (Boolean) -> Unit = {}
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        RequestNotificationPermission(onPermissionResult)
    } else {
        LaunchedEffect(Unit) {
            onPermissionResult(true)
        }
    }
}
