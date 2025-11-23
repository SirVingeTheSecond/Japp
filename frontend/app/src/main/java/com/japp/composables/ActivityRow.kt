package com.japp.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.japp.api.responses.ActivityType
import com.japp.api.responses.activity.ActivityDto
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

fun getActivityIcon(actionType: ActivityType): ImageVector {
    return when (actionType) { // These icons are not final, just placeholders for now
        ActivityType.MEMBER_LEFT -> Icons.Default.Circle
        ActivityType.MEMBER_REMOVED -> Icons.Default.GroupRemove
        ActivityType.GROUP_CREATED -> Icons.Default.Group
        ActivityType.MEMBER_JOINED -> Icons.Default.GroupAdd
        ActivityType.EXPENSE_CREATED -> Icons.Default.Add
        ActivityType.EXPENSE_DELETED -> Icons.Default.Delete
        ActivityType.EXPENSE_UPDATED -> Icons.Default.Update
        ActivityType.RECEIPT_UPLOADED -> Icons.Default.Receipt
        ActivityType.SETTLEMENT_CREATED -> Icons.Default.Hardware
        ActivityType.SETTLEMENT_COMPLETED -> Icons.Default.Done
    }
}

@Composable
fun ActivityRow(
    activity: ActivityDto
) {
    return Row (
        modifier = Modifier
            .padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = getActivityIcon(actionType = activity.actionType),
            contentDescription = "Activity type",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .size(24.dp)
        )

        Text(
            text = activity.userName,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = activity.actionType.description,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.weight(1f))

        PrintableDatetime(
            time = LocalDateTime.ofInstant(Instant.ofEpochMilli(activity.createdAt.toLong()), ZoneId.systemDefault())
        )

    }
}