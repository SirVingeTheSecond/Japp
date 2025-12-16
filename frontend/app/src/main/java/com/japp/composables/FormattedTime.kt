package com.japp.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.japp.utils.DateTimeHelper
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.util.Date

/**
 * Display format options for FormattedTime composable.
 */
enum class TimeFormat {
    /** Relative with "ago" suffix */
    RELATIVE,
    /** Short relative without suffix */
    SHORT,
    /** Full date: "Dec 13, 2024" */
    DATE,
    /** Date and time: "Dec 13, 2024 at 3:30 PM" */
    DATE_TIME,
    /** Time only: "3:30 PM" */
    TIME,
    /** Message: time if today, date otherwise */
    MESSAGE
}

/**
 * Composable for time display.
 *
 * @param timestamp Milliseconds timestamp as string
 * @param format Display format (default: RELATIVE)
 * @param modifier Modifier for the Text composable
 * @param style Text style
 * @param color Text color
 * @param prefix Optional prefix text
 * @param autoUpdate Whether to update periodically (default: true for RELATIVE/SHORT)
 */
@Composable
fun FormattedTime(
    timestamp: String?,
    format: TimeFormat = TimeFormat.RELATIVE,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.labelSmall,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    prefix: String = "",
    autoUpdate: Boolean = format == TimeFormat.RELATIVE || format == TimeFormat.SHORT
) {
    var displayText by remember(timestamp, format) {
        mutableStateOf(formatTimestamp(timestamp, format))
    }

    if (autoUpdate) {
        LaunchedEffect(timestamp, format) {
            while (true) {
                delay(60_000)
                displayText = formatTimestamp(timestamp, format)
            }
        }
    }

    Text(
        text = "$prefix$displayText",
        modifier = modifier,
        style = style,
        color = color
    )
}

private fun formatTimestamp(timestamp: String?, format: TimeFormat): String {
    return when (format) {
        TimeFormat.RELATIVE -> DateTimeHelper.formatRelative(timestamp)
        TimeFormat.SHORT -> DateTimeHelper.formatShortRelative(timestamp)
        TimeFormat.DATE -> DateTimeHelper.formatDate(timestamp)
        TimeFormat.DATE_TIME -> DateTimeHelper.formatDateTime(timestamp)
        TimeFormat.TIME -> DateTimeHelper.formatTime(timestamp)
        TimeFormat.MESSAGE -> DateTimeHelper.formatMessageTime(timestamp)
    }
}

private fun formatDateTime(dateTime: LocalDateTime, format: TimeFormat): String {
    return when (format) {
        TimeFormat.RELATIVE -> DateTimeHelper.formatRelative(dateTime)
        TimeFormat.SHORT -> DateTimeHelper.formatShortRelative(dateTime)
        else -> DateTimeHelper.formatRelative(dateTime)
    }
}
