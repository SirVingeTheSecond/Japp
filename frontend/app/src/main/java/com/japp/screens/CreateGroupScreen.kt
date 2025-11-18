package com.example.japp.screens

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japp.AppDestinations


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateGroupScreen(navController: NavController? = null) {
    var name by remember { mutableStateOf("") }
    var descript by remember { mutableStateOf("") }
    var nameValid by remember { mutableStateOf(true) }
    var descriptValid by remember { mutableStateOf(true) }

    fun createGroup(){
        if (!nameValid && !descriptValid) return
        val call = RetrofitClient.groupService.create_group(
            CreateGroupRequest(
                name,
                descript
            )
        )

        call!!.enqueue(object : Callback<GroupDto?> {
            override fun onResponse(
                call: Call<GroupDto?>,
                response: Response<GroupDto?>
            ) {
                val body = response.body()
                Log.d("Tag", body.toString())

                if (body != null && response.isSuccessful) {
                    navController?.navigate(AppDestinations.HOME.route)
                }

            }

            override fun onFailure(call: Call<GroupDto?>, t: Throwable) {
                Log.d("Tag", t.message!!)
            }
        })
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
                    createGroup()
                }) {
                    Text("Submit")

                }

            }

        }

    }
}

