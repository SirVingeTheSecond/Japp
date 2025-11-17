package com.example.japp.screens


import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import com.example.japp.AppDestinations
import com.example.japp.api.RetrofitClient
import com.example.japp.api.responses.group.GroupDto
import com.example.japp.composables.GroupIcon
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

var GROUP_ID = -1

@Composable
fun GroupScreen(navController: NavController? = null){

    var group by remember { mutableStateOf<GroupDto?>(null) }

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
                }
            }
        }
    }
}
