package com.japp.screens

import android.app.Activity
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.CompoundBarcodeView
import kotlin.random.Random

@Composable
fun ScanScreen(navController: NavController? = null) {
    var cameraAccess by remember { mutableStateOf(false) }

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

    launcher.launch(android.Manifest.permission.CAMERA)


    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        Text(text = "Scan Screen")
        if (cameraAccess) {
            var scanFlag by remember { mutableStateOf(false) }
            var showResult by remember { mutableStateOf(false) }
            var lastReadBarcode by remember { mutableStateOf<String?>(null) }
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
    }

}