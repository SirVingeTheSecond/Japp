package com.japp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.RetrofitClient
import com.japp.api.responses.settlement.CreateSettlementRequest
import com.japp.api.responses.settlement.GroupSettlementSuggestionsDto
import com.japp.api.responses.settlement.SettlementDto
import com.japp.api.responses.settlement.SettlementSuggestionDto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@Composable
fun SettleGroup(
    navController: NavController? = null
) {
    var isLoadingSuggestions by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var suggestionsData by remember { mutableStateOf<GroupSettlementSuggestionsDto?>(null) }
    var userId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        val res = RetrofitClient.userService.getMyUser()
        if (res.isSuccessful && res.body() != null) {
            val user = res.body()!!
            userId = user.id
        } else {
            errorMessage = "Failed to load user info"
        }
    }

    LaunchedEffect(GROUP_ID) {
        if (GROUP_ID == -1) return@LaunchedEffect

        val res = RetrofitClient.settlementService
            .getGroupSettlementSuggestions(GROUP_ID)
        isLoadingSuggestions = false
        if (res.isSuccessful && res.body() != null) {
            suggestionsData = res.body()
        } else {
            errorMessage = "Failed to load settlement suggestions"
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
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

            if (isLoadingSuggestions || userId == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("Loading…")
                }
            } else {
                Text("Debts", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))

                val suggestions: List<SettlementSuggestionDto> =
                    suggestionsData?.suggestions ?: emptyList()

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
                            errorMessage = null

                            val currentUserId = userId
                            if (currentUserId == null) {
                                errorMessage = "User info not loaded yet"
                                return@Button
                            }

                            val mySuggestions =
                                suggestions.filter { it.fromUserId == currentUserId }

                            if (mySuggestions.isEmpty()) {
                                errorMessage =
                                    "You can only settle your own debts. You do not owe anything in this group."
                                return@Button
                            }

                            isSubmitting = true
                            var remaining = mySuggestions.size

                            mySuggestions.forEach { suggestion ->
                                val request = CreateSettlementRequest(
                                    groupId = GROUP_ID,
                                    toUserId = suggestion.toUserId,
                                    amount = suggestion.amount
                                )
                                GlobalScope.launch {
                                    val res = RetrofitClient.settlementService
                                        .createSettlement(request, pendingOnly = true)
                                    remaining -= 1
                                    if (!res.isSuccessful) {
                                        errorMessage = "Failed to create a settlement"
                                    }
                                    if (remaining == 0) {
                                        isSubmitting = false
                                        navController?.navigateUp()
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

            Spacer(Modifier.height(12.dp))

            errorMessage?.let {
                Text(it, color = Color.Red)
            }
        }
    }
}

