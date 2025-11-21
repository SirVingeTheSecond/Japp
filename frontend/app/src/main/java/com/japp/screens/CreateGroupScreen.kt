package com.japp.screens

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.CreateGroupRequest
import com.japp.api.responses.group.GroupDto
import com.japp.composables.GroupIcon
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateGroupScreen(navController: NavController? = null) {
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var descript by remember { mutableStateOf("") }
    var nameValid by remember { mutableStateOf(true) }
    var descriptValid by remember { mutableStateOf(true) }

    suspend fun createGroup(){
        if (!nameValid && !descriptValid) return
        val res = RetrofitClient.groupService.createGroup(
            CreateGroupRequest(
                name,
                descript
            )
        )

        val body = res.body()
        Log.d("Tag", body.toString())

        if (body != null && res.isSuccessful) {
            navController?.navigate(AppDestinations.HOME.route)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        GroupIcon(
            name
        )
        Box(
            Modifier
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(15.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {

                OutlinedTextField(
                    name,
                    isError = !nameValid,
                    onValueChange = {
                        name = it
                        if (it.length > 50 || it.isEmpty()) {
                            nameValid = false
                        } else nameValid = true
                    },
                    label = { Text("Group name") },
                    supportingText = {
                        if (!nameValid) {
                            Text("Name must not be empty and be less than 50 characters.")
                        }
                    }
                )
                OutlinedTextField(
                    descript,
                    isError = !descriptValid,
                    onValueChange = {
                        descript = it
                        if (it.length > 500 || it.isEmpty()) {
                            descriptValid = false
                        } else descriptValid = true
                    },
                    label = { Text("Description") },
                    supportingText = {
                        if (!descriptValid) {
                            Text("Description must not be empty and be less than 500 characters")
                        }
                    }
                )
                Button(onClick = {
                    coroutineScope.launch {createGroup()}
                }) {
                    Text("Submit")

                }

            }

        }

    }
}

