package com.example.japp.screens


import android.graphics.Bitmap
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.example.japp.api.RetrofitClient
import com.example.japp.api.responses.group.GroupDto
import com.example.japp.composables.GroupIcon
import com.google.zxing.BarcodeFormat
import com.journeyapps.barcodescanner.BarcodeEncoder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

var GROUP_ID = -1

@Composable
fun GroupScreen(navController: NavController? = null){
    var qrOpen by remember { mutableStateOf(false) }
    var group by remember { mutableStateOf<GroupDto?>(null) }
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(
        GROUP_ID
    ) {
        if (GROUP_ID == -1) return@LaunchedEffect

        val call = RetrofitClient.groupService.get_group(GROUP_ID)

        call!!.enqueue(object : Callback<GroupDto?> {
            override fun onResponse(
                call: Call<GroupDto?>,
                response: Response<GroupDto?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    group = body
                } else {
                    GROUP_ID = -1
                    navController?.navigateUp()
                }
            }

            override fun onFailure(
                call: Call<GroupDto?>,
                t: Throwable
            ) {
                TODO("Not yet implemented")
            }
        })
    }

    LaunchedEffect(group) {
        if (group != null) {
            qrCode = BarcodeEncoder().encodeBitmap("japp://join/${group!!.id}-${group!!.inviteCode}", BarcodeFormat.QR_CODE, 200, 200)
        }
    }

    Box(

    ){
        Column {
            Box {
                if (group == null){

                }else {
                    Row {
                        GroupIcon(group!!.name)

                        Text(
                            group!!.name,
                            textAlign = TextAlign.Right
                        )
                        group!!.description?.let {
                            Text(
                                it,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                    Button(onClick = { qrOpen = true }) {
                        Text("Show QR!")
                    }
                }
            }
        }
        if (qrOpen) {
            Dialog(onDismissRequest = { qrOpen = false }) {
                // Draw a rectangle shape with rounded corners inside the dialog
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(375.dp)
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (qrCode != null) {
                            Image(
                                bitmap = qrCode!!.asImageBitmap(),
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .fillMaxWidth(0.8f)
                                    .background(Color.Transparent),
                                contentScale = ContentScale.FillBounds,
                                contentDescription = "QR Code for joining group"
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            TextButton(
                                onClick = { qrOpen = false },
                                modifier = Modifier.padding(8.dp),
                            ) {
                                Text("Dismiss")
                            }
                        }
                    }
                }
            }
        }
    }
}
