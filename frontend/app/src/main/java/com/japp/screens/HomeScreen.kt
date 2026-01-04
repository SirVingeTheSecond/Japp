package com.japp.screens

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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.japp.api.safeApiQuery
import com.japp.composables.ErrorWithRetry
import com.japp.composables.GroupIcon
import com.japp.composables.getActivityIcon
import com.japp.ui.state.UiState
import com.japp.ui.theme.LocalExtendedColors
import com.japp.utils.DateTimeHelper
import com.japp.utils.FormatHelper
import com.japp.utils.LocalConnectivity
import java.util.Date
import kotlin.math.absoluteValue

/**
 * Data class for QuickStats.
 */
data class BalanceData(
    val owed: Double,
    val owes: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showSystemUi = true)
@Composable
fun HomeScreen(navController: NavController? = null) {
    var activitiesState by remember { mutableStateOf<UiState<List<ActivityDto>>>(UiState.Loading) }
    var groupsState by remember { mutableStateOf<UiState<List<GroupDto>>>(UiState.Loading) }
    var meState by remember { mutableStateOf<UiState<UserDto>>(UiState.Loading) }
    var balanceState by remember { mutableStateOf<UiState<BalanceData>>(UiState.Loading) }

    var isRefreshing by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    val isConnected = LocalConnectivity.current
    var wasDisconnected by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected && wasDisconnected) {
            refreshKey++
        }
        // IDE does not see that the value is read on the next invocation of the LaunchedEffect
        wasDisconnected = !isConnected
    }

    LaunchedEffect(refreshKey) {
        try {
            when (val result = safeApiQuery("HomeScreen.activities") {
                RetrofitClient.activityService.getUserActivities(limit = 3)
            }) {
                is NetworkResult.Success -> activitiesState = UiState.Success(result.data)
                is NetworkResult.Error -> {
                    if (activitiesState !is UiState.Success) {
                        activitiesState = UiState.Error(result.message)
                    }
                }
            }

            when (val result = safeApiQuery("HomeScreen.groups") {
                RetrofitClient.groupService.getMyGroups()
            }) {
                is NetworkResult.Success -> groupsState = UiState.Success(result.data)
                is NetworkResult.Error -> {
                    if (groupsState !is UiState.Success) {
                        groupsState = UiState.Error(result.message)
                    }
                }
            }

            when (val result = safeApiQuery("HomeScreen.me") {
                RetrofitClient.userService.getMyUser()
            }) {
                is NetworkResult.Success -> meState = UiState.Success(result.data)
                is NetworkResult.Error -> {
                    if (meState !is UiState.Success) {
                        meState = UiState.Error(result.message)
                    }
                }
            }
        } finally {
            isRefreshing = false
        }
    }

    // Calculate balances when groups and user data are available
    LaunchedEffect(groupsState, meState, refreshKey) {
        val groups = groupsState.getOrNull() ?: return@LaunchedEffect
        val me = meState.getOrNull() ?: return@LaunchedEffect

        var totalOwed = 0.0
        var totalOwes = 0.0
        var hasError = false

        for (group in groups) {
            when (val result = safeApiQuery("HomeScreen.balance.${group.id}") {
                RetrofitClient.expenseService.getGroupBalances(group.id)
            }) {
                is NetworkResult.Success -> {
                    val myBal = result.data.balances.find { (userId, _, _) -> userId == me.id }
                    if (myBal != null) {
                        if (myBal.balance < 0) {
                            totalOwes += myBal.balance.absoluteValue
                        } else {
                            totalOwed += myBal.balance.absoluteValue
                        }
                    }
                }
                is NetworkResult.Error -> hasError = true
            }
        }

        if (!hasError || balanceState is UiState.Loading) {
            balanceState = UiState.Success(BalanceData(owed = totalOwed, owes = totalOwes))
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            QuickStats(balanceState)
            Spacer(modifier = Modifier.height(24.dp))
            QuickActivities(navController, activitiesState, onRetry = { refreshKey++ })
            Spacer(modifier = Modifier.height(24.dp))
            QuickGroups(navController, groupsState, meState, onRetry = { refreshKey++ })
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// Reusable Components below here

@Composable
private fun SectionLoadingIndicator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        LinearProgressIndicator(
            modifier = Modifier.width(120.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

@Composable
private fun EmptyStateText(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun SectionHeader(
    title: String,
    actionLabel: String,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        TextButton(onClick = onActionClick) {
            Text(
                text = actionLabel,
                style = MaterialTheme.typography.labelLarge
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun QuickStats(balanceState: UiState<BalanceData>) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(80.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        when (balanceState) {
            is UiState.Loading -> {
                LinearProgressIndicator(
                    modifier = Modifier.width(120.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }

            is UiState.Success -> {
                val extendedColors = LocalExtendedColors.current
                val surfaceColor = MaterialTheme.colorScheme.surface
                val balance = balanceState.data
                val owed = balance.owed
                val owes = balance.owes
                val net = owed - owes

                val positiveColor = Color(ColorUtils.blendARGB(surfaceColor.toArgb(), extendedColors.credit.toArgb(), 0.1f))
                val positiveContentColor = extendedColors.credit
                val negativeColor = Color(ColorUtils.blendARGB(surfaceColor.toArgb(), extendedColors.debt.toArgb(), 0.1f))
                val negativeContentColor = extendedColors.debt
                val neutralColor = MaterialTheme.colorScheme.tertiaryContainer
                val neutralContentColor = MaterialTheme.colorScheme.onTertiaryContainer

                val (netColor, netContentColor) = when {
                    net > 0 -> positiveColor to positiveContentColor
                    net < 0 -> negativeColor to negativeContentColor
                    else -> neutralColor to neutralContentColor
                }

                BalancePill(
                    value = owed,
                    label = "Owed",
                    containerColor = positiveColor,
                    contentColor = positiveContentColor
                )
                BalancePill(
                    value = net,
                    label = "Net",
                    containerColor = netColor,
                    contentColor = netContentColor
                )
                BalancePill(
                    value = -owes,
                    label = "Owes",
                    containerColor = negativeColor,
                    contentColor = negativeContentColor
                )
            }

            is UiState.Error -> {
                Text(
                    text = "Unable to load balances",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun BalancePill(
    value: Double,
    label: String,
    containerColor: Color,
    contentColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(containerColor)
                .padding(vertical = 8.dp, horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = FormatHelper.formatCurrency(value, fractionDigits = 1),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = contentColor
            )
        }
    }
}

@Composable
fun QuickActivities(
    navController: NavController?,
    activitiesState: UiState<List<ActivityDto>>,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SectionHeader(
            title = "Recent activities",
            actionLabel = "Activities",
            onActionClick = { navController?.navigate(AppDestinations.ACTIVITY.route) }
        )

        when (activitiesState) {
            is UiState.Loading -> SectionLoadingIndicator()

            is UiState.Error -> {
                ErrorWithRetry(
                    message = activitiesState.message,
                    onRetry = onRetry
                )
            }

            is UiState.Success -> {
                if (activitiesState.data.isEmpty()) {
                    EmptyStateText("No recent activities")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        activitiesState.data.forEach { activity ->
                            ActivityRow(activity)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActivityRow(activity: ActivityDto) {
    var group by remember { mutableStateOf<GroupDto?>(null) }

    LaunchedEffect(Unit) {
        safeApiQuery("Activity.group.${activity.groupId}") {
            RetrofitClient.groupService.getGroup(activity.groupId)
        }.onSuccess { group = it }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = getActivityIcon(activity.actionType),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = activity.userName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1
                    )
                    Text(
                        text = activity.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
                group?.let { groupData ->
                    Text(
                        text = groupData.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        TimeText(
            date = Date(activity.createdAt.toLong()),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun QuickGroups(
    navController: NavController?,
    groupsState: UiState<List<GroupDto>>,
    meState: UiState<UserDto>,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader(
            title = "Groups",
            actionLabel = "My Groups",
            onActionClick = { navController?.navigate(AppDestinations.MYGROUPS.route) }
        )

        when {
            groupsState is UiState.Loading || meState is UiState.Loading -> {
                SectionLoadingIndicator()
            }

            groupsState is UiState.Error -> {
                ErrorWithRetry(
                    message = groupsState.message,
                    onRetry = onRetry
                )
            }

            meState is UiState.Error -> {
                ErrorWithRetry(
                    message = meState.message,
                    onRetry = onRetry
                )
            }

            groupsState is UiState.Success && meState is UiState.Success -> {
                val groups = groupsState.data
                val me = meState.data

                if (groups.isEmpty()) {
                    EmptyStateText("No groups yet")
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        groups.take(3).forEach { group ->
                            GroupCard(group, me, navController)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: GroupDto,
    me: UserDto,
    navController: NavController? = null
) {
    var groupBalance by remember { mutableStateOf<Double?>(null) }

    LaunchedEffect(Unit) {
        safeApiQuery("Group.balance.${group.id}") {
            RetrofitClient.expenseService.getGroupBalances(group.id)
        }.onSuccess { summaryDto ->
            val myBal = summaryDto.balances.find { (userId, _, _) -> userId == me.id }
            groupBalance = myBal?.balance
        }
    }

    val extendedColors = LocalExtendedColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    val cardColor = if (groupBalance != null) {
        val tintColor = if (groupBalance!! >= 0) extendedColors.credit else extendedColors.debt
        CardDefaults.cardColors(
            containerColor = Color(
                ColorUtils.blendARGB(
                    surfaceColor.toArgb(),
                    tintColor.toArgb(),
                    0.1f
                )
            )
        )
    } else {
        CardDefaults.cardColors()
    }

    Card(
        onClick = { GROUP_ID = group.id; navController?.navigate(AppDestinations.GROUP.route) },
        modifier = Modifier.fillMaxWidth(),
        colors = cardColor,
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GroupIcon(
                content = group.name,
                modifier = Modifier.size(56.dp)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = 1
                )
                if (!group.description.isNullOrBlank()) {
                    Text(
                        text = group.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1
                    )
                }
                Text(
                    text = "${group.memberCount} member${if (group.memberCount != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

@Composable
fun TimeText(
    date: Date,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    textAlign: TextAlign = TextAlign.Start
) {
    Text(
        text = DateTimeHelper.formatRelative(date),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign
    )
}
