package com.japp.screens

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Message
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.japp.AppDestinations
import com.japp.GroupNavController
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.expense.ExpenseDto
import com.japp.api.responses.expense.GroupBalanceSummaryDto
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.GroupMemberDto
import com.japp.api.safeApiQuery
import com.japp.composables.ErrorWithRetry
import com.japp.composables.ExpenseDetailCard
import com.japp.composables.GroupIcon
import com.japp.composables.GroupMemberDetailCard
import com.japp.rememberFabButton
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlin.math.abs

var GROUP_ID = -1

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupScreen(navController: NavController? = null) {
    var qrOpen by remember { mutableStateOf(false) }
    var groupState by remember { mutableStateOf<UiState<GroupDto>>(UiState.Loading) }
    var qrCode by remember { mutableStateOf<Bitmap?>(null) }
    var me by remember { mutableStateOf<UserDto?>(null) }
    val groupMembers = remember { mutableStateOf<List<GroupMemberDto>>(emptyList()) }
    var groupExpenses by remember { mutableStateOf<List<ExpenseDto>>(emptyList()) }
    var groupBalance by remember { mutableStateOf<GroupBalanceSummaryDto?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    // Hook into global FAB (create expense)
    rememberFabButton {
        navController?.navigate(
            groupState.getOrNull()?.let {
                AppDestinations.CustomRoutes.CREATE_EXPENSE.withArgs(it.id)
            } ?: AppDestinations.GROUP.route
        )
    }

    LaunchedEffect(Unit) {
        safeApiQuery("GroupScreen.me") {
            RetrofitClient.userService.getMyUser()
        }.onSuccess { me = it }
    }

    LaunchedEffect(GROUP_ID, refreshKey) {
        if (GROUP_ID == -1) return@LaunchedEffect

        try {
            groupState = UiState.Loading

            groupState = when (val result = safeApiQuery("GroupScreen.group") {
                RetrofitClient.groupService.getGroup(GROUP_ID)
            }) {
                is NetworkResult.Success -> UiState.Success(result.data)
                is NetworkResult.Error -> UiState.Error(result.message)
            }

            if (groupState is UiState.Success) {
                safeApiQuery("GroupScreen.members") {
                    RetrofitClient.groupService.getGroupMembers(GROUP_ID)
                }.onSuccess { groupMembers.value = it }

                safeApiQuery("GroupScreen.balances") {
                    RetrofitClient.expenseService.getGroupBalances(GROUP_ID)
                }.onSuccess { groupBalance = it }

                safeApiQuery("GroupScreen.expenses") {
                    RetrofitClient.expenseService.getGroupExpenses(GROUP_ID)
                }.onSuccess { groupExpenses = it }
            }
        } finally {
            isRefreshing = false
        }
    }

    val group = groupState.getOrNull()

    LaunchedEffect(group) {
        if (group != null) {
            qrCode = BarcodeEncoder().encodeBitmap(
                "japp://join/${group.id}-${group.inviteCode}",
                BarcodeFormat.QR_CODE,
                200,
                200
            )
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            refreshKey++
        }
    ) {
        when (groupState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    ErrorWithRetry(
                        message = (groupState as UiState.Error).message,
                        onRetry = { refreshKey++ }
                    )
                }
            }

            is UiState.Success -> {
                val groupData = (groupState as UiState.Success<GroupDto>).data

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    GroupHeader(
                        group = groupData,
                        groupBalance = groupBalance,
                        me = me,
                        onShowQR = { qrOpen = true },
                        onSettleGroup = {
                            group?.let {
                                navController?.navigate(
                                    AppDestinations.CustomRoutes.SETTLE_GROUP.withArgs(it.id)
                                )
                            }
                        }
                    )

                    NavTab(
                        outerNavController = navController,
                        me = me,
                        groupMembers = groupMembers,
                        groupExpenses = groupExpenses,
                        groupBalance = groupBalance,
                        groupId = GROUP_ID
                    )
                }

                if (qrOpen) {
                    QRCodeDialog(
                        qrCode = qrCode,
                        groupName = groupData.name,
                        onDismiss = { qrOpen = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun GroupHeader(
    group: GroupDto,
    groupBalance: GroupBalanceSummaryDto?,
    me: UserDto?,
    onShowQR: () -> Unit,
    onSettleGroup: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 16.dp, end = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            GroupIcon(content = group.name, modifier = Modifier.size(80.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (!group.description.isNullOrBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        GroupBalanceBar(groupBalance = groupBalance, me = me)

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
        ) {
            FilledTonalButton(onClick = onShowQR) {
                Icon(
                    imageVector = Icons.Outlined.QrCode2,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Show QR")
            }

            Button(onClick = onSettleGroup) {
                Icon(
                    imageVector = Icons.Rounded.Handshake,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settle Group")
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun GroupBalanceBar(
    groupBalance: GroupBalanceSummaryDto?,
    me: UserDto?
) {
    val myBalance = groupBalance?.balances?.find { it.userId == me?.id }?.balance

    val positiveColor = MaterialTheme.colorScheme.primaryContainer
    val positiveContentColor = MaterialTheme.colorScheme.onPrimaryContainer
    val negativeColor = MaterialTheme.colorScheme.errorContainer
    val negativeContentColor = MaterialTheme.colorScheme.onErrorContainer
    val neutralColor = MaterialTheme.colorScheme.tertiaryContainer
    val neutralContentColor = MaterialTheme.colorScheme.onTertiaryContainer

    val (containerColor, contentColor, statusText) = when {
        myBalance == null -> Triple(neutralColor, neutralContentColor, "Loading...")
        myBalance > 0.01 -> Triple(
            positiveColor,
            positiveContentColor,
            "You are owed ${String.format("%.2f", myBalance)}"
        )
        myBalance < -0.01 -> Triple(
            negativeColor,
            negativeContentColor,
            "You owe ${String.format("%.2f", abs(myBalance))}"
        )
        else -> Triple(neutralColor, neutralContentColor, "All settled up")
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .padding(vertical = 12.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
    }
}

@Composable
private fun QRCodeDialog(
    qrCode: Bitmap?,
    groupName: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Join $groupName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                if (qrCode != null) {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    ) {
                        Image(
                            bitmap = qrCode.asImageBitmap(),
                            contentDescription = "QR Code for joining group",
                            modifier = Modifier
                                .size(200.dp)
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.size(200.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator() }
                }

                Text(
                    text = "Scan this code to join the group",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                TextButton(onClick = onDismiss) { Text("Done") }
            }
        }
    }
}

// Reusables

@Composable
private fun EmptyStateMessage(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TabContentContainer(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        content()
        Spacer(modifier = Modifier.height(80.dp)) // space for chat FAB
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavTab(
    outerNavController: NavController?,
    me: UserDto?,
    groupMembers: MutableState<List<GroupMemberDto>>,
    groupExpenses: List<ExpenseDto>,
    groupBalance: GroupBalanceSummaryDto?,
    groupId: Int
) {
    val navController = rememberNavController()
    val snackbar = rememberSnackbar()
    val groupOwner = groupMembers.value.find { it.isOwner }

    var refreshGroupMembersKey by remember { mutableIntStateOf(0) }

    DisposableEffect(navController) {
        GroupNavController.navController = navController
        onDispose { GroupNavController.navController = null }
    }

    LaunchedEffect(refreshGroupMembersKey) {
        when (val result = safeApiQuery("NavTab.refreshMembers") {
            RetrofitClient.groupService.getGroupMembers(GROUP_ID)
        }) {
            is NetworkResult.Success -> groupMembers.value = result.data
            is NetworkResult.Error -> snackbar.showError(result.message)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val selectedDestination = when (currentRoute) {
        "members" -> 0
        "expenses" -> 1
        else -> 0
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            SecondaryTabRow(selectedTabIndex = selectedDestination) {
                Tab(
                    selected = selectedDestination == 0,
                    onClick = { navController.navigate("members") },
                    text = { Text("Members") }
                )
                Tab(
                    selected = selectedDestination == 1,
                    onClick = { navController.navigate("expenses") },
                    text = { Text("Expenses") }
                )
            }

            NavHost(navController = navController, startDestination = "members") {
                composable("members") {
                    MembersTabContent(
                        groupMembers = groupMembers.value,
                        groupBalance = groupBalance,
                        me = me,
                        groupOwner = groupOwner,
                        onRefresh = { refreshGroupMembersKey++ }
                    )
                }

                composable("expenses") {
                    ExpensesTabContent(groupExpenses = groupExpenses)
                }

                composable("chatScreen") {
                    ChatScreen(groupId = groupId)
                }
            }
        }

        // Chat FAB
        AnimatedVisibility(
            visible = currentRoute != "chatScreen",
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            FloatingActionButton(
                onClick = { navController.navigate("chatScreen") },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Message,
                    contentDescription = "Open chat",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MembersTabContent(
    groupMembers: List<GroupMemberDto>,
    groupBalance: GroupBalanceSummaryDto?,
    me: UserDto?,
    groupOwner: GroupMemberDto?,
    onRefresh: () -> Unit
) {
    TabContentContainer {
        if (groupMembers.isEmpty()) {
            EmptyStateMessage("No members")
        } else {
            groupMembers.forEach { memberDto ->
                val balance = groupBalance
                    ?.balances
                    ?.find { it.username == memberDto.username }
                    ?.balance ?: 0.0

                GroupMemberDetailCard(
                    groupId = groupBalance?.groupId ?: 0,
                    groupMember = memberDto,
                    onRefresh = onRefresh,
                    balance = balance,
                    me = me,
                    groupOwner = groupOwner
                )
            }
        }
    }
}

@Composable
private fun ExpensesTabContent(groupExpenses: List<ExpenseDto>) {
    TabContentContainer {
        if (groupExpenses.isEmpty()) {
            EmptyStateMessage("No expenses yet")
        } else {
            groupExpenses.forEach { expense ->
                ExpenseDetailCard(expense = expense)
            }
        }
    }
}
