package com.japp.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.StartupActivity
import com.japp.api.CredentialsStorage
import com.japp.api.ErrorUtils
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.ui.theme.Dimens

@Composable
fun ProfileScreen(navController: NavController) {

    val context = LocalContext.current

    var user by remember { mutableStateOf<UserDto?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val res = RetrofitClient.userService.getMyUser()
        if (res.isSuccessful && res.body() != null) {
            user = res.body()
        } else {
            ErrorUtils.handleError(res, context)
        }
    }

    fun logout() {
        CredentialsStorage.clear(context)

        val intent = Intent(context, StartupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface)
    { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Profile icon",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = user?.firstname ?: "Unknown",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = user?.email ?: "No email",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.secondary
            )
            user?.phone?.let { phone ->
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Spacer(modifier = Modifier.height(Dimens.spacingMedium))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
                onClick = { navController.navigate(AppDestinations.EDITPROFILE.route) }
            ) {
                Text("Edit profile")
            }

            Spacer(modifier = Modifier.height(Dimens.spacingSmall))
            Button(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
                onClick = { "TODO: Implement" }
            ) {
                Text("Settings")
            }


            Spacer(modifier = Modifier.height(Dimens.spacingLarge))

            OutlinedButton(
                onClick = { logout() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.spacingSmall),
            ) {
                Text("Log out")
            }
        }
    }
}
