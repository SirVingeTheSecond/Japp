package com.japp.composables

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun PrintableDatetime(time: LocalDateTime) {

    val minutes = ChronoUnit.MINUTES.between(time, LocalDateTime.now())
    val weeks = ChronoUnit.WEEKS.between(time, LocalDateTime.now())
    val years = ChronoUnit.YEARS.between(time, LocalDateTime.now())

    var res: String

    if (minutes < 1) {
        res = "now"
    } else if (years == 0L && time.dayOfYear == LocalDateTime.now().dayOfYear) {
        res = "today"
    } else if (years == 0L && time.dayOfYear == (LocalDateTime.now().dayOfYear-1)) {
        res = "yesterday"
    } else if (years == 0L && weeks == 0L) {
        res = time.dayOfWeek.name.lowercase()
    } else {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
        res = time.format(dateTimeFormatter)
    }

    Text(
        text = res,
        style = MaterialTheme.typography.labelSmall
    )
}