package com.japp.screens

import android.util.Log
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
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import com.japp.api.RetrofitClient
import com.japp.api.responses.group.GroupDto
import com.japp.composables.GroupIcon
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ShowGroupsScreen(navController: NavController? = null) {
    var groups by remember { mutableStateOf<List<GroupDto>>(emptyList()) }

    suspend fun get_my_groups() {
        val res = RetrofitClient.groupService.getMyGroups()
        if (res.isSuccessful && res.body() != null) {
            groups = res.body()!!
        }
    }

    LaunchedEffect(Unit) {
        get_my_groups()
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        SimpleSearchBar(
            groups = groups,
            onGroupClick = { group ->
                GROUP_ID = group.id
                navController?.navigate(AppDestinations.GROUP.route)
            }
        )
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
            filteredGroups.forEach { group ->
                GroupCard(
                    group = group,
                    onClick = { onGroupClick(group) }
                )
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
            .padding(15.dp)
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
