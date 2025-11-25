package com.japp.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.api.safeApiCall
import com.japp.composables.ActivityRow
import com.japp.composables.ErrorWithRetry
import com.japp.ui.state.UiState
import com.japp.utils.LocalConnectivity

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
fun ActivityScreen(navController: NavController? = null) {
    var activitiesState by remember { mutableStateOf<UiState<List<ActivityDto>>>(UiState.Loading) }
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
        when (val result = safeApiCall("ActivityScreen.activities") {
            RetrofitClient.activityService.getUserActivities(limit = null)
        }) {
            is NetworkResult.Success -> activitiesState = UiState.Success(result.data)
            is NetworkResult.Error -> {
                if (activitiesState !is UiState.Success) {
                    activitiesState = UiState.Error(result.message)
                }
                // else keep cached Success state
            }
        }
        isRefreshing = false
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshKey++
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (activitiesState) {
                is UiState.Loading -> {
                    CircularProgressIndicator()
                }

                is UiState.Error -> {
                    ErrorWithRetry(
                        message = (activitiesState as UiState.Error).message,
                        onRetry = { refreshKey++ }
                    )
                }

                is UiState.Success -> {
                    val activities = (activitiesState as UiState.Success<List<ActivityDto>>).data
                    if (activities.isEmpty()) {
                        Text(
                            text = "No activities yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        activities.forEach { activity ->
                            ActivityRow(activity)
                        }
                    }
                }
            }
        }
    }
}
