package com.japp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val res = RetrofitClient.userService.getMyUser()
        if (res.isSuccessful && res.body() != null) {
            val u = res.body()!!
            firstname = u.firstname
            lastname = u.lastname
            phone = u.phone ?: ""
            profilePicture = u.profilePicture ?: ""
        }
    }

    fun save() {
        loading = true
        scope.launch {
            val req = UpdateUserRequest(
                firstname = firstname,
                lastname = lastname,
                phone = phone.ifBlank { null },
                profilePicture = profilePicture.ifBlank { null }
            )

            val res = RetrofitClient.userService.updateMyUser(req)
            loading = false

            if (res.isSuccessful) {
                navController.popBackStack()
            } else {
                errorMessage = "Failed to update"
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

            if (errorMessage != null) {
                Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(12.dp))
            }

            Button(
                onClick = { save() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !loading
            ) {
                Text(if (loading) "Saving..." else "Save")
            }
        }
    }
}
