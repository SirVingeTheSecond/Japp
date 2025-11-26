package com.japp.screens

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupPreviewDto
import com.japp.api.responses.group.JoinGroupRequest
import com.japp.api.safeApiMutation
import com.japp.api.safeApiQuery
import com.japp.composables.GroupIcon
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlinx.coroutines.launch
import kotlin.random.Random

@Composable
fun ScanScreen(navController: NavController? = null) {
    val context = LocalContext.current

    var cameraAccess by remember { mutableStateOf(false) }
    var scanFlag by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var lastReadBarcode by remember { mutableStateOf<String?>(null) }

    // camera access popup
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            cameraAccess = true
        } else {
            ActivityResultContracts.RequestPermission()
        }
    }

    LaunchedEffect(Unit) {
        launcher.launch(android.Manifest.permission.CAMERA)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Scan Screen")
        if (cameraAccess) {
            var torchState by remember { mutableStateOf(false) }
            var recomposeFlag by remember { mutableIntStateOf(Random.nextInt()) }
            key(recomposeFlag) {
                AndroidView(
                    factory = { context ->
                        val preview = CompoundBarcodeView(context)
                        preview.setStatusText("")
                        preview.cameraSettings.isAutoTorchEnabled = torchState
                        preview.apply {
                            val capture = CaptureManager(context as Activity, this)
                            capture.initializeFromIntent(context.intent, null)
                            capture.decode()
                            this.decodeContinuous { result ->
                                if (scanFlag) {
                                    return@decodeContinuous
                                }
                                scanFlag = true
                                result.text?.let { barCodeOrQr ->
                                    lastReadBarcode = result.text
                                    scanFlag = true
                                    showResult = true
                                }
                            }
                            this.resume()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        if (showResult && lastReadBarcode != null) {
            val barcodeMatch = "(?<=(japp://join/))(\\d+)-([A-Z0-9]+)".toRegex()
            val matches = barcodeMatch.find(lastReadBarcode!!)
            val groupId = matches?.groupValues?.get(2)
            val inviteCode = matches?.groupValues?.get(3)

            if (groupId != null && inviteCode != null) {
                ScanResultDialog(
                    inviteCode = inviteCode,
                    onDismiss = {
                        showResult = false
                        scanFlag = false
                    },
                    onJoinSuccess = { joinedGroupId ->
                        GROUP_ID = joinedGroupId
                        navController?.navigate(AppDestinations.GROUP.route)
                    }
                )
            } else {
                showResult = false
                scanFlag = false
                Toast.makeText(context, "Invalid QR code", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ScanResultDialog(
    inviteCode: String,
    onDismiss: () -> Unit,
    onJoinSuccess: (Int) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var groupState by remember { mutableStateOf<UiState<GroupPreviewDto>>(UiState.Loading) }
    var isJoining by remember { mutableStateOf(false) }

    LaunchedEffect(inviteCode) {
        groupState = when (val result = safeApiQuery("ScanScreen.groupPreview") {
            RetrofitClient.groupService.getGroup(inviteCode)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    fun joinGroup() {
        isJoining = true

        coroutineScope.launch {
            when (val result = safeApiMutation("ScanScreen.join") {
                RetrofitClient.groupService.joinGroup(JoinGroupRequest(inviteCode))
            }) {
                is NetworkResult.Success -> {
                    val groupName = (groupState as? UiState.Success)?.data?.name ?: "the group"
                    snackbar.showSuccess("Joined $groupName!")
                    onJoinSuccess(result.data.id)
                }
                is NetworkResult.Error -> {
                    snackbar.showError(result.message, onRetry = { joinGroup() })
                    isJoining = false
                }
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card {
            Column(
                Modifier.padding(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (groupState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(Modifier.padding(16.dp))
                    }
                    is UiState.Error -> {
                        Text(
                            text = (groupState as UiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                    is UiState.Success -> {
                        val group = (groupState as UiState.Success<GroupPreviewDto>).data

                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                GroupIcon(group.name, Modifier.size(90.dp))
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    group.name,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                HorizontalDivider(
                                    Modifier
                                        .padding(5.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                    thickness = 2.dp
                                )
                                Text(group.description ?: "No description")
                                HorizontalDivider(
                                    Modifier
                                        .padding(5.dp)
                                        .background(MaterialTheme.colorScheme.primary),
                                    thickness = 2.dp
                                )
                                Text("${group.memberCount} members")
                            }
                        }

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(top = 5.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            // on left cuz left handed people rule!
                            Button(
                                onClick = { joinGroup() },
                                enabled = !isJoining
                            ) {
                                Text(if (isJoining) "Joining..." else "Join ${group.name}")
                            }
                            Button(onClick = onDismiss) {
                                Text("Cancel")
                            }
                        }
                    }
                }
            }
        }
    }
}
