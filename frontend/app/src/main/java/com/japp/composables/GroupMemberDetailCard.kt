package com.japp.composables

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.ColorUtils
import com.japp.api.ErrorUtils
import com.japp.api.RetrofitClient
import com.japp.api.responses.auth.UserDto
import com.japp.api.responses.group.GroupMemberDto
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun GroupMemberDetailCard(
    groupId: Int,
    groupMember: GroupMemberDto,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    balance: Double? = null,
    me: UserDto? = null,
    groupOwner: GroupMemberDto? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val isOwner = groupMember.userId == groupOwner?.userId

    var expanded by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf<UserDto?>(null) }
    var kickGroupMember by remember { mutableStateOf<GroupMemberDto?>(null) }

    var colorTint = Color.Transparent
    var cardColor = CardDefaults.cardColors()
    if (balance != null && balance != 0.0) {
        colorTint = if (balance >= 0) Color(0xFF20DF6C) else Color(0xFFDF2020)
        cardColor = CardDefaults.cardColors(
            Color(ColorUtils.blendARGB(cardColor.containerColor.toArgb(), colorTint.toArgb(), 0.1f))
        )
    }

    LaunchedEffect(Unit) {
        val res = RetrofitClient.userService.getUser(groupMember.userId)
        if (res.isSuccessful && res.body() != null) {
            user = res.body()
        } else {
            ErrorUtils.handleError(res, context)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { expanded = !expanded },
        colors = cardColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    groupMember.username + if (isOwner) " (Owner)" else "",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    (((balance ?: 0.0) * 100.0).roundToInt() / 100.0).toString(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(4.dp))

            // Basic info
            user?.let { dto ->
                Text(
                    "Full Name: ${dto.firstname} ${dto.lastname}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (expanded) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column() {
                        Text(
                            "Joined at: ${groupMember.joinedAt.takeIf { it.isNotEmpty() } ?: "Unknown"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Number: ${groupMember.userEmail}",
                            style = MaterialTheme.typography.bodySmall
                        )

                        Text(
                            "Number: ${user?.phone}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    if (groupOwner?.userId == me?.id && groupMember.userId != me?.id) {
                        Column(
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Button(
                                { kickGroupMember = groupMember }
                            ) {
                                if (groupMember.username.length < 12) {
                                    Text("Kick ${groupMember.username}")
                                } else {
                                    Text("Kick")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    kickGroupMember?.let {
        Dialog(
            { kickGroupMember = null }
        ) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(10.dp).fillMaxWidth(),
                ) {
                    Text(
                        "Are you sure you wish to kick ${kickGroupMember!!.username}?",
                        Modifier.fillMaxWidth(),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "They won't be able to return unless someone invites them back.",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        "Whatever debt they have will be gone.",
                        Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        OutlinedButton({ kickGroupMember = null }) {
                            Text("No, go back.")
                        }
                        Button({
                            coroutineScope.launch {
                                if (kickGroupMember != null) {
                                    val res = RetrofitClient.groupService.kickGroupMember(groupId, kickGroupMember!!.userId)
                                    if (res.isSuccessful) {
                                        kickGroupMember = null
                                        onRefresh()
                                    } else {
                                        ErrorUtils.handleError(res, context)
                                    }
                                }
                            }
                        }) {
                            if (kickGroupMember!!.username.length < 12) {
                                Text("Goodbye ${kickGroupMember!!.username}!")
                            } else {
                                Text("Goodbye!")
                            }
                        }
                    }
                }
            }
        }
    }
}