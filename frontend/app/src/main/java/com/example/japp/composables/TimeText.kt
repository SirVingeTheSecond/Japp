package com.example.japp.composables

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import java.text.DateFormat
import java.util.Date

@Composable
fun TimeText(date: Date, modifier: Modifier = Modifier) {
    val dFormat = DateFormat.getDateInstance()
    var text = ""
    val timeSince = (Date().time - date.time) / 1000

    if (timeSince < 60) {
        // Seconds ago
        text = "$timeSince seconds ago"
    } else if (timeSince < 60*60) {
        // Minutes ago
        text = "${timeSince/60} minutes ago"
    } else if (timeSince < 60*60*24) {
        // Hours ago
        text = "${timeSince/60/60} hours ago"
    } else if (timeSince < 60*60*24*7) {
        // Days ago
        text = "${timeSince/60/60/24} days ago"
    } else if (timeSince < 60*60*24*14) {
        // 1 week ago??
        text = "over 1 week ago"
    } else {
        text = dFormat.format(date)
    }

    Text(
        text,
        modifier = modifier
    )
}