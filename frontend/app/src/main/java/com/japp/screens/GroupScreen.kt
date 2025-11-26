package com.japp.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.expense.ExpenseDto
import com.japp.api.responses.expense.GroupBalanceSummaryDto
import com.japp.api.responses.group.GroupDto
import com.japp.api.responses.group.GroupMemberDto
import com.japp.api.safeApiCall
import com.japp.api.safeApiMutation
import com.japp.api.safeApiQuery
import com.japp.composables.ErrorWithRetry
import com.japp.composables.ExpenseDetailCard
import com.japp.composables.GroupIcon
import com.japp.composables.GroupMemberDetailCard
import com.japp.rememberFabButton
import com.japp.ui.rememberSnackbar
import com.japp.ui.state.UiState
import com.journeyapps.barcodescanner.BarcodeEncoder
import kotlinx.coroutines.launch

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

    // Hook into action button
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

        groupState = UiState.Loading

        // Group details
        groupState = when (val result = safeApiQuery("GroupScreen.group") {
            RetrofitClient.groupService.getGroup(GROUP_ID)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        // Only fetch secondary data if group loaded successfully
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

        isRefreshing = false
    }

    val group = groupState.getOrNull()

    LaunchedEffect(group) {
        if (group != null) {
            // Create QR code
            qrCode = BarcodeEncoder().encodeBitmap(
                "japp://join/${group.id}-${group.inviteCode}",
                BarcodeFormat.QR_CODE,
                200,
                200
            )
        }
    }

    Box {
        when (groupState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                // Seems logical to allow refresh on error
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
                            message = (groupState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }
                }
            }

            is UiState.Success -> {
                val groupData = (groupState as UiState.Success<GroupDto>).data

                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshKey++
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Row {
                            GroupIcon(
                                groupData.name,
                                modifier = Modifier.padding(12.dp)
                            )
                            Column(
                                modifier = Modifier.padding(15.dp)
                            ) {
                                Text(
                                    groupData.name,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = MaterialTheme.typography.titleLarge,
                                    textAlign = TextAlign.Right,
                                )
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )
                                groupData.description?.let {
                                    Text(
                                        it, textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Button(
                                onClick = { qrOpen = true },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("Show QR")
                            }

                            Button(
                                onClick = {
                                    group?.let {
                                        navController?.navigate(
                                            AppDestinations.CustomRoutes.SETTLE_GROUP.withArgs(it.id)
                                        )
                                    }
                                },
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                Text("Settle Group")
                            }
                        }

                        NavTab(navController, me, groupMembers, groupExpenses, groupBalance, GROUP_ID)
                    }
                }

                if (qrOpen) {
                    Dialog(onDismissRequest = { qrOpen = false }) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(375.dp)
                                .padding(16.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (qrCode != null) {
                                    Image(
                                        bitmap = qrCode!!.asImageBitmap(),
                                        modifier = Modifier
                                            .aspectRatio(1f)
                                            .fillMaxWidth(0.8f)
                                            .background(Color.Transparent),
                                        contentScale = ContentScale.FillBounds,
                                        contentDescription = "QR Code for joining group"
                                    )
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                ) {
                                    TextButton(
                                        onClick = { qrOpen = false },
                                        modifier = Modifier.padding(8.dp),
                                    ) {
                                        Text("Dismiss")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()
    val groupOwner = groupMembers.value.find { dto -> dto.isOwner }

    var refreshGroupMembersKey by remember { mutableIntStateOf(0) }
    var leaving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

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
        "1" -> 0
        "2" -> 1
        "3" -> 2
        "4" -> 3
        else -> 0
    }

    Column {
        SecondaryTabRow(
            selectedTabIndex = selectedDestination,
        ) {
            Tab(
                selected = selectedDestination == 0,
                onClick = { navController.navigate("1") }
            ) {
                Text("Members", Modifier.padding(10.dp))
            }
            Tab(
                selected = selectedDestination == 1,
                onClick = { navController.navigate("2") }
            ) {
                Text("Expenses", Modifier.padding(10.dp))
            }
            Tab(
                selected = selectedDestination == 2,
                onClick = { navController.navigate("3") }
            ) {
                Text("Chat", Modifier.padding(10.dp))
            }
            Tab(
                selected = selectedDestination == 3,
                onClick = { navController.navigate("4") }
            ) {
                Text("Options", Modifier.padding(10.dp))
            }
        }
        NavHost(
            navController = navController,
            startDestination = "1"
        ) {
            composable("1") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(
                            state = rememberScrollState(),
                            enabled = true,
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    groupMembers.value.forEach { memberDto ->
                        val balance = groupBalance
                            ?.balances
                            ?.find { it.username == memberDto.username }
                            ?.balance ?: 0.0
                        GroupMemberDetailCard(
                            groupBalance?.groupId ?: 0,
                            memberDto,
                            { refreshGroupMembersKey++ },
                            balance = balance,
                            me = me,
                            groupOwner = groupOwner
                        )
                    }
                }
            }

            composable("2") {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .verticalScroll(
                            state = rememberScrollState(),
                            enabled = true,
                        )
                        .padding(horizontal = 8.dp)
                ) {
                    if (groupExpenses.isEmpty()) {
                        Spacer(Modifier.height(32.dp))
                        Text(
                            "No expenses yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        groupExpenses.forEach { expense ->
                            ExpenseDetailCard(expense = expense)
                        }
                    }
                }
            }

            composable("3") {
                ChatScreen(groupId = groupId)
            }

            composable("4") {
                var notificationsEnabled by remember { mutableStateOf(false) }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text("Settings", style = MaterialTheme.typography.headlineSmall)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Text(
                            "Enable notifications",
                            modifier = Modifier.weight(1f)
                        )

                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                    HorizontalDivider()
                    Text("Actions", style = MaterialTheme.typography.headlineSmall)
                    Text(
                        "Be careful",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Button(
                        onClick = { leaving = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .padding(top = 24.dp)
                    ) {
                        Text("LEAVE GROUP")
                    }
                    if (groupOwner?.userId == me?.id) {
                        Button(
                            onClick = { deleting = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.Red,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .padding(top = 24.dp)
                        ) {
                            Text("DELETE GROUP")
                        }
                    }
                }
            }
        }
    }

    // Dialogs
    when {
        leaving -> {
            Dialog({ leaving = false }) {
                Card {
                    Column(
                        Modifier.padding(10.dp)
                    ) {
                        Text(
                            "Are you sure you wish to leave?",
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            OutlinedButton({ leaving = false }) {
                                Text("No!")
                            }
                            Button({
                                coroutineScope.launch {
                                    when (val result = safeApiMutation("NavTab.leaveGroup") {
                                        RetrofitClient.groupService.leaveGroup(groupId)
                                    }) {
                                        is NetworkResult.Success -> {
                                            snackbar.showSuccess("Left the group")
                                            outerNavController?.popBackStack(
                                                AppDestinations.HOME.route,
                                                false
                                            )
                                        }

                                        is NetworkResult.Error -> {
                                            snackbar.showError(result.message)
                                            leaving = false
                                        }
                                    }
                                }
                            }) {
                                Text("Yes!")
                            }
                        }
                    }
                }
            }
        }

        deleting -> {
            Dialog({ deleting = false }) {
                Card {
                    Column(
                        Modifier.padding(10.dp)
                    ) {
                        Text(
                            "Are you sure you wish to delete this group?",
                            Modifier
                                .fillMaxWidth()
                                .padding(5.dp),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            "There is no undoing this.",
                            Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            OutlinedButton({ deleting = false }) {
                                Text("No!")
                            }
                            Button({
                                coroutineScope.launch {
                                    when (val result = safeApiMutation("NavTab.deleteGroup") {
                                        RetrofitClient.groupService.deleteGroup(groupId)
                                    }) {
                                        is NetworkResult.Success -> {
                                            snackbar.showSuccess("Group deleted")
                                            outerNavController?.popBackStack(
                                                AppDestinations.HOME.route,
                                                false
                                            )
                                        }

                                        is NetworkResult.Error -> {
                                            snackbar.showError(result.message)
                                            deleting = false
                                        }
                                    }
                                }
                            }) {
                                Text("Yes!")
                            }
                        }
                    }
                }
            }
        }
    }
}
