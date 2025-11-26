package com.japp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.user.UpdateUserRequest
import com.japp.api.safeApiCall
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var userState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }

    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        userState = when (val result = safeApiCall("EditProfile.load") {
            RetrofitClient.userService.getMyUser()
        }) {
            is NetworkResult.Success -> {
                val user = result.data
                firstname = user.firstname
                lastname = user.lastname
                phone = user.phone.orEmpty()
                profilePicture = user.profilePicture.orEmpty()
                UiState.Success(user)
            }
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    fun save() {
        if (saving) return
        saving = true

        scope.launch {
            val request = UpdateUserRequest(
                firstname = firstname,
                lastname = lastname,
                phone = phone.ifBlank { null },
                profilePicture = profilePicture.ifBlank { null }
            )

            when (val result = safeApiCall("EditProfile.save") {
                RetrofitClient.userService.updateMyUser(request)
            }) {
                is NetworkResult.Success -> {
                    snackbar.showSuccess("Profile updated!")
                    navController.popBackStack()
                }
                is NetworkResult.Error -> {
                    snackbar.showError(result.message, onRetry = { save() })
                    saving = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Profile") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { padding ->
        when (userState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (userState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is UiState.Success -> {
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.Top
                ) {
                    OutlinedTextField(
                        value = firstname,
                        onValueChange = { firstname = it },
                        label = { Text("First name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = { Text("Last name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = profilePicture,
                        onValueChange = { profilePicture = it },
                        label = { Text("Profile picture URL") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { save() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving
                    ) {
                        Text(if (saving) "Saving..." else "Save")
                    }
                }
            }
        }
    }
}
