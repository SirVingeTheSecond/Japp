package com.japp.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.CreateGroupRequest
import com.japp.api.safeApiCall
import com.japp.composables.GroupIcon
import com.japp.ui.rememberSnackbar
import kotlinx.coroutines.launch


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CreateGroupScreen(navController: NavController? = null) {
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var name by remember { mutableStateOf("") }
    var descript by remember { mutableStateOf("") }
    var nameValid by remember { mutableStateOf(true) }
    var descriptValid by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    fun createGroup() {
        if (!nameValid || !descriptValid) return
        if (name.isEmpty()) {
            nameValid = false
            return
        }

        isSubmitting = true

        coroutineScope.launch {
            when (val result = safeApiCall("CreateGroupScreen.create") {
                RetrofitClient.groupService.createGroup(
                    CreateGroupRequest(name, descript)
                )
            }) {
                is NetworkResult.Success -> {
                    snackbar.showSuccess("Group \"$name\" created!")
                    navController?.navigate(AppDestinations.HOME.route)
                }
                is NetworkResult.Error -> {
                    snackbar.showError(result.message, onRetry = { createGroup() })
                    isSubmitting = false
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically)
    ) {
        GroupIcon(name)

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
                        nameValid = it.isNotEmpty() && it.length <= 50
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
                        descriptValid = it.length <= 500
                    },
                    label = { Text("Description") },
                    supportingText = {
                        if (!descriptValid) {
                            Text("Description must be less than 500 characters")
                        }
                    }
                )

                Button(
                    onClick = { createGroup() },
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                    }
                    Text(if (isSubmitting) "Creating..." else "Submit")
                }
            }
        }
    }
}
