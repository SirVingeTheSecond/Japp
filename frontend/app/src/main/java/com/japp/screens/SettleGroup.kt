package com.japp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.settlement.CreateSettlementRequest
import com.japp.api.responses.settlement.GroupSettlementSuggestionsDto
import com.japp.api.responses.settlement.SettlementDto
import com.japp.api.safeApiCall
import com.japp.composables.ErrorWithRetry
import com.japp.composables.SlideToConfirm
import com.japp.ui.state.UiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleGroup(
    navController: NavController? = null,
    groupId: Int? = null
) {
    val coroutineScope = rememberCoroutineScope()

    var suggestionsState by remember { mutableStateOf<UiState<GroupSettlementSuggestionsDto>>(UiState.Loading) }
    var pendingSettlementsState by remember { mutableStateOf<UiState<List<SettlementDto>>>(UiState.Loading) }
    var currentUserId by remember { mutableStateOf<Int?>(null) }

    var isCreatingSettlements by remember { mutableStateOf(false) }
    var createError by remember { mutableStateOf<String?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        if (groupId == null || groupId == -1) {
            suggestionsState = UiState.Error("Invalid group ID")
            pendingSettlementsState = UiState.Error("Invalid group ID")
            isRefreshing = false
            return@LaunchedEffect
        }

        // Load user
        val userResult = safeApiCall("SettleGroup.user") {
            RetrofitClient.userService.getMyUser()
        }
        when (userResult) {
            is NetworkResult.Success -> currentUserId = userResult.data.id
            is NetworkResult.Error -> {
                suggestionsState = UiState.Error(userResult.message)
                isRefreshing = false
                return@LaunchedEffect
            }
        }

        // Load suggestions
        suggestionsState = when (val result = safeApiCall("SettleGroup.suggestions") {
            RetrofitClient.settlementService.getGroupSettlementSuggestions(groupId)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        // Load pending settlements
        pendingSettlementsState = when (val result = safeApiCall("SettleGroup.pending") {
            RetrofitClient.settlementService.getGroupSettlements(groupId, pendingOnly = true)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        isRefreshing = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when {
            suggestionsState is UiState.Loading || pendingSettlementsState is UiState.Loading -> {
                CircularProgressIndicator()
            }

            suggestionsState is UiState.Error -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshKey++
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorWithRetry(
                            message = (suggestionsState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }
                }
            }

            pendingSettlementsState is UiState.Error -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshKey++
                    }
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        ErrorWithRetry(
                            message = (pendingSettlementsState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }
                }
            }

            currentUserId == null -> {
                ErrorWithRetry(
                    message = "Failed to load user information",
                    onRetry = { refreshKey++ }
                )
            }

            else -> {
                val suggestions = (suggestionsState as UiState.Success<GroupSettlementSuggestionsDto>).data.suggestions
                val pendingSettlements = (pendingSettlementsState as UiState.Success<List<SettlementDto>>).data
                val userId = currentUserId!!

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshKey++
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Settle Group", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(16.dp))

                        // Section 1: Payments to Confirm
                        val pendingToConfirm = pendingSettlements.filter { it.toUserId == userId }

                        if (pendingToConfirm.isNotEmpty()) {
                            Text(
                                "Payments to Confirm",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Confirm when you have received payment",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))

                            pendingToConfirm.forEach { settlement ->
                                key (settlement.id) {
                                    var expanded by remember { mutableStateOf(false) }
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clickable(onClick = { expanded = !expanded }),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(16.dp)
                                        ) {
                                            Row(
                                                Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column() {
                                                    Text(
                                                        "${settlement.fromUserName} paid you",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                                    )
                                                    Text(
                                                        "${settlement.amount} DKK",
                                                        style = MaterialTheme.typography.headlineSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                Text(
                                                    if (expanded) "Tap to collapse" else "Tap to expand",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            if (expanded) {
                                                Spacer(Modifier.height(8.dp))

                                                SlideToConfirm(
                                                    onConfirm = {
                                                        coroutineScope.launch {
                                                            when (safeApiCall("SettleGroup.complete") {
                                                                RetrofitClient.settlementService.completeSettlement(settlement.id)
                                                            }) {
                                                                is NetworkResult.Success -> {
                                                                    refreshKey++
                                                                }
                                                                is NetworkResult.Error -> {
                                                                    createError = "Failed to confirm payment"
                                                                }
                                                            }
                                                        }
                                                    },
                                                    trackColor = MaterialTheme.colorScheme.onSecondary,
                                                    text = "Slide to confirm received",
                                                    enabled = !isCreatingSettlements
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // Section 2: Pending Settlements User Created
                        val pendingByUser = pendingSettlements.filter { it.fromUserId == userId }

                        if (pendingByUser.isNotEmpty()) {
                            Text(
                                "Your Pending Payments",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                "Waiting for recipient confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))

                            pendingByUser.forEach { settlement ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "Paid ${settlement.toUserName}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                            Text(
                                                "${settlement.amount} DKK",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                        Text(
                                            "Pending",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.tertiary
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(24.dp))
                        }

                        // Section 3: Debt Overview
                        Text("Debt Overview", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))

                        if (suggestions.isEmpty()) {
                            Text(
                                "No debts in this group",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            suggestions.forEach { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(

                                    ) {
                                        Text(
                                            suggestion.fromUserName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                        Icon(
                                            Icons.AutoMirrored.Default.ArrowRight,
                                            ""
                                        )
                                        Text(
                                            suggestion.toUserName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    }
                                    Text(
                                        "${suggestion.amount} DKK",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                            }

                            Spacer(Modifier.height(16.dp))

                            val mySuggestions = suggestions.filter { it.fromUserId == userId }

                            if (mySuggestions.isNotEmpty()) {
                                Button(
                                    onClick = {
                                        createError = null
                                        isCreatingSettlements = true

                                        coroutineScope.launch {
                                            var errorOccurred = false

                                            mySuggestions.forEach { suggestion ->
                                                val request = CreateSettlementRequest(
                                                    groupId = groupId!!,
                                                    toUserId = suggestion.toUserId,
                                                    amount = suggestion.amount
                                                )

                                                when (safeApiCall("SettleGroup.create") {
                                                    RetrofitClient.settlementService.createSettlement(
                                                        request,
                                                        pendingOnly = true
                                                    )
                                                }) {
                                                    is NetworkResult.Success -> {}
                                                    is NetworkResult.Error -> {
                                                        errorOccurred = true
                                                        createError = "Failed to create settlements"
                                                    }
                                                }
                                            }

                                            isCreatingSettlements = false

                                            if (!errorOccurred) {
                                                refreshKey++
                                            }
                                        }
                                    },
                                    enabled = !isCreatingSettlements,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        if (isCreatingSettlements) "Recording Payments..."
                                        else "Record My Payments (${mySuggestions.size})"
                                    )
                                }
                            } else {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer
                                    )
                                ) {
                                    Text(
                                        "You do not owe anything in this group",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        createError?.let {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Text(
                                    text = it,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
