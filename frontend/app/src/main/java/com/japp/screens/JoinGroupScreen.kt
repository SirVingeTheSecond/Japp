package com.japp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.JoinGroupRequest
import com.japp.composables.GroupIcon
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun JoinGroupScreen(navController: NavController? = null, inviteCode: String?) {
    if (inviteCode == null) {
        navController?.navigateUp()
        return
    }
    val groupId = inviteCode.split("-").first()
    val code = inviteCode.split("-").last()

    // Fetch group!
    var group by remember { mutableStateOf<GroupDto?>(null) }
    LaunchedEffect(Unit) {
        val call = RetrofitClient.groupService.get_group(groupId.toInt())
        call!!.enqueue(object: Callback<GroupDto?>{
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

        })
    }

    fun join_group() {
        val call = RetrofitClient.groupService.join_group(JoinGroupRequest(code))

        call!!.enqueue(object: Callback<GroupDto?>{
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

        })
    }

    Box (
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            group?.let { group ->
                val gradientColors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.tertiary)
                Text("You have been invited to join:", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Text(
                    group.name.uppercase(),
                    style = TextStyle(
                        brush = Brush.linearGradient(
                            colors = gradientColors
                        ),
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize
                    )
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    GroupIcon(group.name)
//                    Text(group.name, style = MaterialTheme.typography.headlineMedium)
                    Text("Member count: ${group.memberCount}")
                }
            }
            Button({ join_group() }) {
                Text("Join group!")
            }
        }
    }
}