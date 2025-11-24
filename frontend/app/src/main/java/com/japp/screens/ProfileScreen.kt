package com.japp.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.StartupActivity
import com.japp.api.CredentialsStorage
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.safeApiCall
import com.japp.composables.ErrorWithRetry
import com.japp.ui.state.UiState
import com.japp.ui.theme.Dimens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {

    val context = LocalContext.current

    var userState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        userState = when (val result = safeApiCall("ProfileScreen.user") {
            RetrofitClient.userService.getMyUser()
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }
        isRefreshing = false
    }

    fun logout() {
        CredentialsStorage.clear(context)

        val intent = Intent(context, StartupActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface)
    { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                isRefreshing = true
                refreshKey++
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
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

                when (userState) {
                    is UiState.Loading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    is UiState.Error -> {
                        ErrorWithRetry(
                            message = (userState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }

                    is UiState.Success -> {
                        val user = (userState as UiState.Success<UserDto>).data

                        Text(
                            text = user.firstname,
                            style = MaterialTheme.typography.titleLarge
                        )

                        Text(
                            text = user.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        user.phone?.let { phone ->
                            Text(
                                text = phone,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(Dimens.spacingMedium))
                Button(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Dimens.spacingSmall),
                    onClick = { navController.navigate(AppDestinations.EDITPROFILE.route) },
                    enabled = userState is UiState.Success
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
}
