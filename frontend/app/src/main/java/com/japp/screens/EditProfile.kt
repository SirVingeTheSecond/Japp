package com.japp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.user.UpdateUserRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }

    var saving by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val userLoadState by produceState<Result<Unit>?>(initialValue = null) {
        value = try {
            val res = RetrofitClient.userService.getMyUser()
            if (res.isSuccessful && res.body() != null) {
                val u = res.body()!!
                firstname = u.firstname
                lastname = u.lastname
                phone = u.phone.orEmpty()
                profilePicture = u.profilePicture.orEmpty()
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to load user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun save() {
        if (saving) return
        saving = true
        errorMessage = null

        scope.launch {
            try {
                val req = UpdateUserRequest(
                    firstname = firstname,
                    lastname = lastname,
                    phone = phone.ifBlank { null },
                    profilePicture = profilePicture.ifBlank { null }
                )

                val res = RetrofitClient.userService.updateMyUser(req)
                saving = false

                if (res.isSuccessful) {
                    navController.popBackStack()
                } else {
                    val err = res.errorBody()?.string()
                    errorMessage = err ?: "Failed to update"
                }
            } catch (e: Exception) {
                saving = false
                errorMessage = "Network error"
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

        when {
            userLoadState == null -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            userLoadState?.isFailure == true -> {
                Box(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Failed to load user")
                }
            }

            else -> {
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

                    errorMessage?.let {
                        Text(it, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(12.dp))
                    }

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
