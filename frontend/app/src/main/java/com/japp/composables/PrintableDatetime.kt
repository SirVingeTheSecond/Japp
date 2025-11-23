package com.japp.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun PrintableDatetime(time: LocalDateTime) {
    var now by remember { mutableStateOf(LocalDateTime.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60000) // Update every minute
            now = LocalDateTime.now()
        }
    }

    // Calculate relative time
    val minutes = ChronoUnit.MINUTES.between(time, now)
    val hours = ChronoUnit.HOURS.between(time, now)
    val weeks = ChronoUnit.WEEKS.between(time, now)
    val years = ChronoUnit.YEARS.between(time, now)

    val res: String = when {
        minutes < 1 -> "now"
        minutes < 60 -> "${minutes}m"
        hours <= 24 && years == 0L && time.dayOfYear == now.dayOfYear -> "${hours}h"
        years == 0L && time.dayOfYear == (now.dayOfYear - 1) -> "yesterday"
        years == 0L && weeks == 0L -> time.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
        else -> {
            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
            time.format(dateTimeFormatter)
        }
    }

    Text(
        text = res,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}
