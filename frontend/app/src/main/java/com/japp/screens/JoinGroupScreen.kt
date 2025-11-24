package com.japp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupPreviewDto
import com.japp.api.responses.group.JoinGroupRequest
import com.japp.api.safeApiCall
import com.japp.composables.GroupIcon
import com.japp.ui.state.UiState
import kotlinx.coroutines.launch

@Composable
fun JoinGroupScreen(navController: NavController? = null, inviteCode: String?) {
    if (inviteCode == null) {
        navController?.navigateUp()
        return
    }

    val code = inviteCode.split("-").last()
    val coroutineScope = rememberCoroutineScope()

    var groupState by remember { mutableStateOf<UiState<GroupPreviewDto>>(UiState.Loading) }
    var isJoining by remember { mutableStateOf(false) }
    var joinError by remember { mutableStateOf<String?>(null) }

    // Fetch group!
    LaunchedEffect(Unit) {
        groupState = when (val result = safeApiCall("JoinGroupScreen.group") {
            RetrofitClient.groupService.getGroup(inviteCode)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    fun joinGroup() {
        isJoining = true
        joinError = null

        coroutineScope.launch {
            when (val result = safeApiCall("JoinGroupScreen.join") {
                RetrofitClient.groupService.joinGroup(JoinGroupRequest(code))
            }) {
                is NetworkResult.Success -> {
                    GROUP_ID = result.data.id
                    navController?.navigate(AppDestinations.GROUP.route)
                }
                is NetworkResult.Error -> {
                    joinError = result.message
                    isJoining = false
                }
            }
        }
    }

    Box(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        when (groupState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Error -> {
                Text(
                    text = (groupState as UiState.Error).message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(16.dp)
                )
            }
            is UiState.Success -> {
                val group = (groupState as UiState.Success<GroupPreviewDto>).data
                val gradientColors = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(30.dp)
                ) {
                    Text(
                        "You have been invited to join:",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        group.name.uppercase(),
                        style = TextStyle(
                            brush = Brush.linearGradient(colors = gradientColors),
                            fontSize = MaterialTheme.typography.headlineLarge.fontSize
                        )
                    )
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        GroupIcon(group.name)
                        Text("Member count: ${group.memberCount}")
                    }

                    joinError?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    Button(
                        onClick = { joinGroup() },
                        enabled = !isJoining
                    ) {
                        Text(if (isJoining) "Joining..." else "Join group!")
                    }
                }
            }
        }
    }
}
