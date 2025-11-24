package com.japp.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.graphics.ColorUtils
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.activity.ActivityDto
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.group.GroupDto
import com.japp.api.safeApiCall
import com.japp.composables.ErrorWithRetry
import com.japp.composables.GroupIcon
import com.japp.composables.getActivityIcon
import com.japp.ui.state.UiState
import java.text.SimpleDateFormat
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.math.round
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
fun HomeScreen(navController: NavController? = null) {
    var activitiesState by remember { mutableStateOf<UiState<List<ActivityDto>>>(UiState.Loading) }
    var groupsState by remember { mutableStateOf<UiState<List<GroupDto>>>(UiState.Loading) }
    var meState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }

    var owed by remember { mutableStateOf<Double?>(null) }
    var owes by remember { mutableStateOf<Double?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        // Activity call
        activitiesState = when (val result = safeApiCall("HomeScreen.activities") {
            RetrofitClient.activityService.getUserActivities(limit = 3)
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        // Group call
        groupsState = when (val result = safeApiCall("HomeScreen.groups") {
            RetrofitClient.groupService.getMyGroups()
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        // Me call
        meState = when (val result = safeApiCall("HomeScreen.me") {
            RetrofitClient.userService.getMyUser()
        }) {
            is NetworkResult.Success -> UiState.Success(result.data)
            is NetworkResult.Error -> UiState.Error(result.message)
        }

        isRefreshing = false
    }

    LaunchedEffect(groupsState, meState, refreshKey) {
        val groups = groupsState.getOrNull() ?: return@LaunchedEffect
        val me = meState.getOrNull() ?: return@LaunchedEffect

        // Calculate balances across all groups
        var totalOwed = 0.0
        var totalOwes = 0.0

        for (group in groups) {
            val result = safeApiCall("HomeScreen.balance.${group.id}") {
                RetrofitClient.expenseService.getGroupBalances(group.id)
            }
            if (result is NetworkResult.Success) {
                val balanceSummaryDto = result.data
                val myBal = balanceSummaryDto.balances.find { (userId, _, _) -> userId == me.id }
                if (myBal != null) {
                    if (myBal.balance < 0) totalOwes += myBal.balance.absoluteValue else totalOwed += myBal.balance.absoluteValue
                }
            }
            // Silently skip failed balance fetches to avoid blocking the entire screen
        }

        owed = totalOwed
        owes = totalOwes
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
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            QuickStats(owed, owes)
            HorizontalDivider(
                Modifier
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.primary),
                thickness = 2.dp
            )
            QuickActivities(navController, activitiesState, onRetry = { refreshKey++ })
            HorizontalDivider(
                Modifier
                    .padding(10.dp)
                    .background(MaterialTheme.colorScheme.primary),
                thickness = 2.dp
            )
            QuickGroups(navController, groupsState, meState, onRetry = { refreshKey++ })
        }
    }
}

@Composable
fun QuickStats(
    owed: Double?,
    owes: Double?
) {
    var ratio by remember { mutableStateOf<Double?>(null) }
    var difference by remember { mutableStateOf<Double?>(null) }
    Row(
        Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        if (owed != null && owes != null) {
            ratio = if (owed + owes > 0) round((owes / (owed + owes)) * 100) / 100 else 0.0
            difference = owed - owes
            val acceptColor = Color(0xFF20DF6C)
            val errorColor = Color(0xFFDF2020)
            val ratioColorInt =
                ratio?.let {
                    ColorUtils.blendARGB(
                        acceptColor.toArgb(),
                        errorColor.toArgb(),
                        it.toFloat()
                    )
                }
            val ratioColor = Color(ratioColorInt ?: Color.Yellow.toArgb())

            Pill(
                ((owed * 10).roundToInt() / 10.0).toString(),
                label = "Owed",
                color = acceptColor,
                textColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Pill(
                ((difference!! * 10).roundToInt() / 10.0).toString(),
                label = "Ratio",
                color = ratioColor,
                textColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Pill(
                ((-owes * 10).roundToInt() / 10.0).toString(),
                label = "Owes",
                color = errorColor,
                textColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
        } else {
            LinearProgressIndicator(
                Modifier.align(Alignment.CenterVertically),
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun Pill(
    content: String = "Idk?",
    label: String? = null,
    color: Color? = null,
    textColor: Color? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        label?.let { Text(it) }
        Box(
            Modifier
                .clip(RoundedCornerShape(100.dp))
                .background(color ?: MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 6.dp, horizontal = 12.dp)
        ) {
            Text(content, color = textColor ?: MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
fun QuickActivities(
    navController: NavController?,
    activitiesState: UiState<List<ActivityDto>>,
    onRetry: () -> Unit  // ADD this parameter
) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Recent activities", style = MaterialTheme.typography.headlineSmall)
            TextButton(
                onClick = { navController?.navigate(AppDestinations.ACTIVITY.route) }
            ) {
                Text("Activities ->", textAlign = TextAlign.End)
            }
        }

        when (activitiesState) {
            is UiState.Loading -> {
                LinearProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            is UiState.Error -> {
                ErrorWithRetry(
                    message = activitiesState.message,
                    onRetry = onRetry
                )
            }

            is UiState.Success -> {
                if (activitiesState.data.isEmpty()) {
                    Text(
                        text = "No recent activities",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    for (activity in activitiesState.data) {
                        Activity(activity)
                    }
                }
            }
        }
    }
}

@Composable
fun Activity(activity: ActivityDto) {
    var group by remember { mutableStateOf<GroupDto?>(null) }

    LaunchedEffect(Unit) {
        safeApiCall("Activity.group.${activity.groupId}") {
            RetrofitClient.groupService.getGroup(activity.groupId)
        }.onSuccess { group = it }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                getActivityIcon(activity.actionType),
                contentDescription = "Icon",
            )
            Text(
                activity.userName,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                activity.description,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
            Text(
                group?.name ?: "",
                overflow = TextOverflow.MiddleEllipsis,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1
            )
        }
        TimeText(
            Date(activity.createdAt.toLong()),
            Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.End
        )
    }
}

@Composable
fun QuickGroups(
    navController: NavController?,
    groupsState: UiState<List<GroupDto>>,
    meState: UiState<UserDto>,
    onRetry: () -> Unit  // ADD this parameter
) {
    Column(
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Groups", style = MaterialTheme.typography.headlineSmall)
            TextButton(
                onClick = { navController?.navigate(AppDestinations.MYGROUPS.route) }
            ) {
                Text("My Groups ->", textAlign = TextAlign.End)
            }
        }

        when {
            groupsState is UiState.Loading || meState is UiState.Loading -> {
                LinearProgressIndicator(
                    Modifier.align(Alignment.CenterHorizontally),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            groupsState is UiState.Error -> {
                // REPLACE Text with ErrorWithRetry
                ErrorWithRetry(
                    message = groupsState.message,
                    onRetry = onRetry
                )
            }

            meState is UiState.Error -> {
                // REPLACE Text with ErrorWithRetry
                ErrorWithRetry(
                    message = meState.message,
                    onRetry = onRetry
                )
            }

            groupsState is UiState.Success && meState is UiState.Success -> {
                val groups = groupsState.data
                val me = meState.data

                if (groups.isEmpty()) {
                    Text(
                        text = "No groups yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    for (group in (if (groups.size <= 3) groups else groups.slice(
                        IntRange(
                            0,
                            2
                        )
                    ))) {
                        Group(group, me, navController)
                    }
                }
            }
        }
    }
}

@Composable
fun Group(group: GroupDto, me: UserDto, navController: NavController? = null) {
    var groupBalance by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        safeApiCall("Group.balance.${group.id}") {
            RetrofitClient.expenseService.getGroupBalances(group.id)
        }.onSuccess { summaryDto ->
            val myBal = summaryDto.balances.find { (userId, _, _) -> userId == me.id }
            groupBalance = myBal?.balance
        }
    }

    var colorTint = Color.Transparent
    var cardColor = CardDefaults.cardColors()
    if (groupBalance != null) {
        colorTint = if (groupBalance!! >= 0) Color(0xFF20DF6C) else Color(0xFFDF2020)
        cardColor = CardDefaults.cardColors(
            Color(
                ColorUtils.blendARGB(
                    cardColor.containerColor.toArgb(),
                    colorTint.toArgb(),
                    0.1f
                )
            )
        )
    }

    Card(
        onClick = { GROUP_ID = group.id; navController?.navigate(AppDestinations.GROUP.route) },
        modifier = Modifier.height(100.dp),
        colors = cardColor
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                GroupIcon(group.name, Modifier.size(75.dp))
            }
            Column {
                Text(group.name, overflow = TextOverflow.Ellipsis, maxLines = 1)
                HorizontalDivider()
                if (group.description != null) {
                    Text(group.description, style = MaterialTheme.typography.labelSmall)
                    HorizontalDivider()
                }
                Text(
                    "Group members: ${group.memberCount}",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

// Extra composables
@SuppressLint("SimpleDateFormat")
@Composable
fun TimeText(
    date: Date,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    textAlign: TextAlign = TextAlign.Start
) {
    val time = (Date().time - date.time) / 1000
    var timeText = ""
    val minute = 60
    val hour = minute * 60
    val day = hour * 24
    val week = day * 7
    val biWeekly = week * 2

    if (time < minute) {
        timeText = "${time}s ago"
    } else if (time < hour) {
        timeText = "${time / minute}m ago"
    } else if (time < day) {
        timeText = "${time / hour}h ago"
    } else if (time < week) {
        timeText = "${time / day}d ago"
    } else if (time < biWeekly) {
        timeText = "over a week ago"
    } else {
        timeText = SimpleDateFormat("dd/mm/yy").format(date)
    }

    Text(timeText, modifier = modifier.then(Modifier), style = style, textAlign = textAlign)
}
