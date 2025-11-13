package com.example.japp.screens

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
import androidx.compose.foundation.layout.paddingFrom
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.navigation.NavController
import com.example.japp.AppDestinations
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


fun printableDatetime(time: LocalDateTime): String {

    val minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now())
    val weeks = ChronoUnit.WEEKS.between(time, LocalDateTime.now())
    val years = ChronoUnit.YEARS.between(time, LocalDateTime.now())

    if (minutes < 1) {
        return "now"
    } else if (years == 0L && time.dayOfYear == LocalDateTime.now().dayOfYear) {
        return "today"
    } else if (years == 0L && time.dayOfYear == (LocalDateTime.now().dayOfYear-1)) {
        return "yesterday"
    } else if (years == 0L && weeks == 0L) {
        return time.dayOfWeek.name
    } else {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
        return time.format(dateTimeFormatter)
    }
}

@Composable
fun ActivityRow(
    icon: ImageVector,
    user: String,
    action: String,
    date: LocalDateTime
) {
    return Row (
            modifier = Modifier
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = "Activity type",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(24.dp)
            )

            Text(
                text = user,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = action,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = printableDatetime(date),
                style = MaterialTheme.typography.labelSmall
            )
        }
}

@Preview(showSystemUi = true)
@Composable
fun ActivityScreen() {
    Scaffold (
        modifier = Modifier
            .background(MaterialTheme.colorScheme.primary)
    ) { paddingValues -> Column (
        modifier = Modifier
            .fillMaxWidth()
            .padding(paddingValues)
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ActivityRow(icon = Icons.Default.Circle, user = "Mikkel", action = "added a receipt for 10$ to test group", date = LocalDateTime.now() )
            ActivityRow(icon = Icons.Default.Circle, user = "Mikkel", action = "added a receipt for 20$ to test group", date = LocalDateTime.of(2025,11,12,12,0) )
            ActivityRow(icon = Icons.Default.Circle, user = "Mikkel", action = "added a receipt for 30$ to test group", date = LocalDateTime.of(2025, 10, 1, 12, 0) )
        }

    }
}