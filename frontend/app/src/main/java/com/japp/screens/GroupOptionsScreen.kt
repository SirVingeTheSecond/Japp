package com.japp.screens

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.japp.AppDestinations
import com.japp.api.NetworkResult
import com.japp.api.RetrofitClient
import com.japp.api.safeApiMutation
import com.japp.api.safeApiQuery
import com.japp.ui.rememberSnackbar
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupOptionsScreen(
    navController: NavController,
    groupId: Int
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbar = rememberSnackbar()

    // notification toggle state (not persisted)
    var notificationsEnabled by remember { mutableStateOf(true) }

    // Owner check state
    var isOwner by remember { mutableStateOf(false) }

    // Dialog states
    var showLeaveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    // Check if current user is owner
    LaunchedEffect(groupId) {
        val meResult = safeApiQuery("GroupOptionsScreen.me") {
            RetrofitClient.userService.getMyUser()
        }
        val membersResult = safeApiQuery("GroupOptionsScreen.members") {
            RetrofitClient.groupService.getGroupMembers(groupId)
        }

        if (meResult is NetworkResult.Success && membersResult is NetworkResult.Success) {
            val me = meResult.data
            val owner = membersResult.data.find { it.isOwner }
            isOwner = owner?.userId == me.id
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        GroupOptionsScreenContent(
            modifier = Modifier.padding(padding),
            isOwner = isOwner,
            onLeaveGroup = { showLeaveDialog = true },
            onDeleteGroup = { showDeleteDialog = true },
            notificationsEnabled = notificationsEnabled,
            onNotificationsEnabledChange = { notificationsEnabled = it }
        )
    }

    // Leave group
    if (showLeaveDialog) {
        ConfirmationDialog(
            title = "Leave Group?",
            message = "Are you sure you want to leave this group?",
            confirmText = "Leave",
            isLoading = isProcessing,
            onConfirm = {
                isProcessing = true
                coroutineScope.launch {
                    when (val result = safeApiMutation("GroupOptionsScreen.leaveGroup") {
                        RetrofitClient.groupService.leaveGroup(groupId)
                    }) {
                        is NetworkResult.Success -> {
                            snackbar.showSuccess("Left the group")
                            navController.popBackStack(AppDestinations.HOME.route, false)
                        }
                        is NetworkResult.Error -> {
                            snackbar.showError(result.message)
                            isProcessing = false
                            showLeaveDialog = false
                        }
                    }
                }
            },
            onDismiss = { showLeaveDialog = false }
        )
    }

    // Delete group
    if (showDeleteDialog) {
        ConfirmationDialog(
            title = "Delete Group?",
            message = "Are you sure you want to delete this group?",
            subtitle = "This action cannot be undone.",
            confirmText = "Delete",
            isDestructive = true,
            isLoading = isProcessing,
            onConfirm = {
                isProcessing = true
                coroutineScope.launch {
                    when (val result = safeApiMutation("GroupOptionsScreen.deleteGroup") {
                        RetrofitClient.groupService.deleteGroup(groupId)
                    }) {
                        is NetworkResult.Success -> {
                            snackbar.showSuccess("Group deleted")
                            navController.popBackStack(AppDestinations.HOME.route, false)
                        }
                        is NetworkResult.Error -> {
                            snackbar.showError(result.message)
                            isProcessing = false
                            showDeleteDialog = false
                        }
                    }
                }
            },
            onDismiss = { showDeleteDialog = false }
        )
    }
}

@Composable
private fun GroupOptionsScreenContent(
    modifier: Modifier = Modifier,
    isOwner: Boolean,
    onLeaveGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    notificationsEnabled: Boolean,
    onNotificationsEnabledChange: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Notifications section
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Notifications",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable notifications",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Get notified about new expenses and settlements",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = notificationsEnabled,
                        onCheckedChange = onNotificationsEnabledChange
                    )
                }
            }
        }

        HorizontalDivider()

        // Highway to the DANGER ZONE
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Danger Zone",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = "These actions are irreversible",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onLeaveGroup,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Leave Group")
            }

            if (isOwner) {
                Button(
                    onClick = onDeleteGroup,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Group")
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun ConfirmationDialog(
    title: String,
    message: String,
    subtitle: String? = null,
    confirmText: String,
    isDestructive: Boolean = false,
    isLoading: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isLoading) onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                subtitle?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        enabled = !isLoading
                    ) {
                        Text("Cancel")
                    }

                    if (isDestructive) {
                        Button(
                            onClick = onConfirm,
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(if (isLoading) "Deleting..." else confirmText)
                        }
                    } else {
                        Button(
                            onClick = onConfirm,
                            enabled = !isLoading
                        ) {
                            Text(if (isLoading) "Leaving..." else confirmText)
                        }
                    }
                }
            }
        }
    }
}
