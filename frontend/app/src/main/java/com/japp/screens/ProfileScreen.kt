package com.japp.screens

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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.SessionManager
import com.japp.api.responses.auth.UserDto
import com.japp.api.safeApiQuery
import com.japp.composables.ErrorWithRetry
import com.japp.ui.state.UiState
import com.japp.utils.LocalConnectivity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val context = LocalContext.current

    var userState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }
    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val isConnected = LocalConnectivity.current
    var wasDisconnected by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected && wasDisconnected) {
            refreshKey++
        }
        wasDisconnected = !isConnected
    }

    LaunchedEffect(refreshKey) {
        when (val result = safeApiQuery("ProfileScreen.user") {
            RetrofitClient.userService.getMyUser()
        }) {
            is NetworkResult.Success -> userState = UiState.Success(result.data)
            is NetworkResult.Error -> {
                if (userState !is UiState.Success) {
                    userState = UiState.Error(result.message)
                }
            }
        }
        isRefreshing = false
    }

    Scaffold(containerColor = MaterialTheme.colorScheme.surface) { padding ->
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
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                when (userState) {
                    is UiState.Loading -> {
                        Spacer(modifier = Modifier.height(48.dp))
                        CircularProgressIndicator()
                    }

                    is UiState.Error -> {
                        Spacer(modifier = Modifier.height(48.dp))
                        ErrorWithRetry(
                            message = (userState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }

                    is UiState.Success -> {
                        val user = (userState as UiState.Success<UserDto>).data

                        ProfileAvatar(
                            profilePicture = user.profilePicture,
                            userId = user.id
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Text(
                            text = "${user.firstname} ${user.lastname}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = "@${user.username}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        ProfileInfoSection(user = user)

                        Spacer(modifier = Modifier.height(32.dp))

                        Button(
                            onClick = { navController.navigate(AppDestinations.EDITPROFILE.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Edit Profile")
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        FilledTonalButton(
                            onClick = { SessionManager.logout(context) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Logout,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.size(8.dp))
                            Text("Log Out")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profilePicture: String?,
    userId: Int
) {
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (profilePicture != null) {
            val imageUrl = "${RetrofitClient.BASE_URL}user/$userId/pp"
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile picture",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Default profile icon",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(56.dp)
            )
        }
    }
}

@Composable
private fun ProfileInfoSection(user: UserDto) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        ProfileInfoRow(label = "Email", value = user.email)
        user.phone?.let { phone ->
            if (phone.isNotBlank()) {
                ProfileInfoRow(label = "Phone", value = phone)
            }
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Text(
        text = value,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
