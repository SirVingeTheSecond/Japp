package com.example.japp.screens

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.japp.composables.GroupIcon

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateGroupScreen(navController: NavController? = null) {
    var name by remember { mutableStateOf("") }
    var descript by remember { mutableStateOf("") }
    var nameValid by remember { mutableStateOf(true) }
    var descriptValid by remember { mutableStateOf(true) }
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
        ) {
            GroupIcon(name)
            Box(
                Modifier.background(MaterialTheme.colorScheme.surfaceContainer).padding(15.dp),
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
                    Button(onClick = {}) {
                        Text("Submit")
                        //TODO add endpoints
                    }

                }

            }

    }
}

