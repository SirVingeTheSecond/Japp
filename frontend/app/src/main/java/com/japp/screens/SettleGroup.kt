package com.japp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.settlement.CreateSettlementRequest
import com.japp.api.responses.settlement.GroupSettlementSuggestionsDto
import com.japp.api.responses.settlement.SettlementSuggestionDto
import com.japp.api.safeApiMutation
import com.japp.api.safeApiQuery
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import kotlinx.coroutines.launch

@Composable
fun SettleGroup(
    navController: NavController? = null
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    var suggestionsState by remember { mutableStateOf<UiState<GroupSettlementSuggestionsDto>>(UiState.Loading) }
    var userId by remember { mutableStateOf<Int?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        safeApiQuery("SettleGroup.user") {
            RetrofitClient.userService.getMyUser()
        }.onSuccess { userId = it.id }
    }

    LaunchedEffect(GROUP_ID) {
        if (GROUP_ID == -1) return@LaunchedEffect

        suggestionsState = when (val result = safeApiQuery("SettleGroup.suggestions") {
            RetrofitClient.settlementService.getGroupSettlementSuggestions(GROUP_ID)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Settle Group", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))

            when {
                suggestionsState is UiState.Loading || userId == null -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(12.dp))
                        Text("Loading…")
                    }
                }
                suggestionsState is UiState.Error -> {
                    Text(
                        text = (suggestionsState as UiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                suggestionsState is UiState.Success -> {
                    val suggestionsData = (suggestionsState as UiState.Success<GroupSettlementSuggestionsDto>).data
                    val suggestions: List<SettlementSuggestionDto> = suggestionsData.suggestions

                    Text("Debts", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))

                    if (suggestions.isEmpty()) {
                        Text("No debts.")
                    } else {
                        suggestions.forEach { suggestion ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Text(
                                    "${suggestion.fromUserName} → ${suggestion.toUserName}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    "Amount: ${suggestion.amount}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            HorizontalDivider(
                                thickness = 1.dp,
                                color = Color.LightGray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        Spacer(Modifier.height(12.dp))

                        Button(
                            onClick = {
                                val currentUserId = userId
                                if (currentUserId == null) {
                                    snackbar.showError("User info not loaded yet")
                                    return@Button
                                }

                                val mySuggestions = suggestions.filter { it.fromUserId == currentUserId }

                                if (mySuggestions.isEmpty()) {
                                    snackbar.showError("You can only settle your own debts. You do not owe anything in this group.")
                                    return@Button
                                }

                                isSubmitting = true
                                var remaining = mySuggestions.size
                                var hasError = false

                                mySuggestions.forEach { suggestion ->
                                    val request = CreateSettlementRequest(
                                        groupId = GROUP_ID,
                                        toUserId = suggestion.toUserId,
                                        amount = suggestion.amount
                                    )
                                    coroutineScope.launch {
                                        when (val result = safeApiMutation("SettleGroup.create") {
                                            RetrofitClient.settlementService.createSettlement(request, pendingOnly = true)
                                        }) {
                                            is NetworkResult.Success -> { /* Settlement created */ }
                                            is NetworkResult.Error -> {
                                                hasError = true
                                                snackbar.showError(result.message)
                                            }
                                        }
                                        remaining -= 1
                                        if (remaining == 0) {
                                            isSubmitting = false
                                            if (!hasError) {
                                                snackbar.showSuccess("Settlements created!")
                                                navController?.navigateUp()
                                            }
                                        }
                                    }
                                }
                            },
                            enabled = !isSubmitting,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(if (isSubmitting) "Creating…" else "Settle Group")
                        }
                    }
                }
            }
        }
    }
}
