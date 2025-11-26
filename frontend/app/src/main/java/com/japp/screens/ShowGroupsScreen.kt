package com.japp.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupDto
import com.japp.api.safeApiQuery
import com.japp.composables.ErrorWithRetry
import com.japp.composables.GroupIcon
import com.japp.ui.state.UiState
import com.japp.utils.LocalConnectivity

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShowGroupsScreen(navController: NavController? = null) {
    var groupsState by remember { mutableStateOf<UiState<List<GroupDto>>>(UiState.Loading) }
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
        when (val result = safeApiQuery("ShowGroupsScreen.groups") {
            RetrofitClient.groupService.getMyGroups()
        }) {
            is NetworkResult.Success -> groupsState = UiState.Success(result.data)
            is NetworkResult.Error -> {
                if (groupsState !is UiState.Success) {
                    groupsState = UiState.Error(result.message)
                }
            }
        }
        isRefreshing = false
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (groupsState) {
            is UiState.Loading -> {
                CircularProgressIndicator()
            }
            is UiState.Error -> {
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
                            message = (groupsState as UiState.Error).message,
                            onRetry = { refreshKey++ }
                        )
                    }
                }
            }
            is UiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = {
                        isRefreshing = true
                        refreshKey++
                    }
                ) {
                    SimpleSearchBar(
                        groups = (groupsState as UiState.Success<List<GroupDto>>).data,
                        onGroupClick = { group ->
                            GROUP_ID = group.id
                            navController?.navigate(AppDestinations.GROUP.route)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleSearchBar(
    groups: List<GroupDto>,
    onGroupClick: (GroupDto) -> Unit,
    modifier: Modifier = Modifier
) {
    val textFieldState = rememberTextFieldState()
    var expanded by rememberSaveable { mutableStateOf(false) }

    val query = textFieldState.text.toString()
    val filteredGroups = remember(groups, query) {
        if (query.isBlank()) groups
        else groups.filter { it.name.contains(query, ignoreCase = true) }
    }

    Column(
        modifier
            .fillMaxSize()
            .semantics { isTraversalGroup = true }
    ) {
        SearchBar(
            modifier = Modifier.fillMaxWidth(),
            inputField = {
                SearchBarDefaults.InputField(
                    query = query,
                    onQueryChange = { newText ->
                        textFieldState.edit {
                            replace(0, length, newText)
                        }
                    },
                    onSearch = {
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Search") }
                )
            },
            expanded = expanded,
            onExpandedChange = { expanded = it },
        ) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                filteredGroups.forEach { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        modifier = Modifier
                            .clickable {
                                textFieldState.edit {
                                    replace(0, length, group.name)
                                }
                                expanded = false
                                onGroupClick(group)
                            }
                            .fillMaxWidth()
                    )
                }
            }
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (filteredGroups.isEmpty()) {
                Text(
                    text = if (query.isBlank()) "No groups yet" else "No groups match \"$query\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.CenterHorizontally)
                )
            } else {
                filteredGroups.forEach { group ->
                    GroupCard(
                        group = group,
                        onClick = { onGroupClick(group) }
                    )
                }
            }
        }
    }
}

@Composable
fun GroupCard(
    group: GroupDto,
    onClick: () -> Unit
) {
    ElevatedCard(
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp
        ),
        modifier = Modifier
            .padding(10.dp)
            .fillMaxWidth()
            .height(100.dp),
        onClick = {
            onClick()
        }
    ) {
        Row {
            GroupIcon(
                group.name,
                Modifier.size(100.dp)
            )
            Column {
                Text(
                    text = group.name,
                    modifier = Modifier.padding(15.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
