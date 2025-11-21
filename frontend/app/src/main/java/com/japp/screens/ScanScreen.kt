package com.japp.screens

import android.app.Activity
import android.util.Log
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
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.JoinGroupRequest
import com.japp.composables.GroupIcon
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.random.Random

@Composable
fun ScanScreen(navController: NavController? = null) {
    var cameraAccess by remember { mutableStateOf(false) }
    var scanFlag by remember { mutableStateOf(false) }
    var showResult by remember { mutableStateOf(false) }
    var lastReadBarcode by remember { mutableStateOf<String?>(null) }

    // camera access popup
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Log.d("msg","CAM ACCESS GRANTED")
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
            key (recomposeFlag){
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
                    modifier = Modifier
                        .fillMaxSize()
                )
            }
        }

        fun JoinGroup(inviteCode: String) {
            RetrofitClient.groupService.join_group(JoinGroupRequest(inviteCode))!!.enqueue(
                object: Callback<GroupDto?> {
                    override fun onResponse(
                        call: Call<GroupDto?>,
                        response: Response<GroupDto?>
                    ) {
                        val body = response.body()
                        if (body != null && response.isSuccessful) {
                            GROUP_ID = body.id
                            navController?.navigate(AppDestinations.GROUP.route)
                        }
                    }

                    override fun onFailure(
                        call: Call<GroupDto?>,
                        t: Throwable
                    ) {
                        TODO("Not yet implemented")
                    }
                }
            )
        }

        if (showResult) {
            val barcodeMatch = "(?<=(japp://join/))(\\d+)-([A-Z0-9]+)".toRegex()
            val matches = barcodeMatch.find(lastReadBarcode!!)
            val groupId  = matches?.groupValues?.get(2)
            val inviteCode = matches?.groupValues?.get(3)
            if (groupId != null && inviteCode != null) {
                var group by remember { mutableStateOf<GroupDto?>(null) }
                LaunchedEffect(Unit) {
                    RetrofitClient.groupService.get_group(groupId.toInt())!!.enqueue(
                        object: Callback<GroupDto?> {
                            override fun onResponse(
                                call: Call<GroupDto?>,
                                response: Response<GroupDto?>
                            ) {
                                val body = response.body()
                                if (body != null && response.isSuccessful) {
                                    group = body
                                }
                            }

                            override fun onFailure(
                                call: Call<GroupDto?>,
                                t: Throwable
                            ) {
                                TODO("Not yet implemented")
                            }

                        }
                    )
                }
                Dialog({showResult = false; scanFlag = false}) {
                    Card() {
                        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                if (group != null) {
                                    Box(Modifier.padding(10.dp), contentAlignment = Alignment.Center) {
                                        GroupIcon(group!!.name, Modifier.size(90.dp))
                                    }
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(group!!.name, style = MaterialTheme.typography.headlineMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        HorizontalDivider(
                                            Modifier
                                                .padding(5.dp)
                                                .background(MaterialTheme.colorScheme.primary),
                                            thickness = 2.dp
                                        )
                                        Text(group!!.description ?: "No description")
                                        HorizontalDivider(
                                            Modifier
                                                .padding(5.dp)
                                                .background(MaterialTheme.colorScheme.primary),
                                            thickness = 2.dp
                                        )
                                        Text("${group!!.memberCount} members")
                                    }
                                }
                            }
                            Row(Modifier.fillMaxWidth().padding(top = 5.dp), horizontalArrangement = Arrangement.SpaceAround) {
                                // on left cuz left handed people rule!
                                Button({JoinGroup(inviteCode)}) {
                                    Text("Join ${group?.name ?:"..."}")
                                }
                                Button({showResult = false; scanFlag = false}) {
                                    Text("Cancel")
                                }
                            }
                        }
                    }
                }

            } else {
                showResult = false
                scanFlag = false
                Toast.makeText(LocalContext.current, "Invalid", Toast.LENGTH_SHORT).show()
            }
        }
    }
}