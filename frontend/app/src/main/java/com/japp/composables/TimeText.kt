package com.japp.composables

import android.util.Log
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import kotlinx.coroutines.delay
import java.text.DateFormat
import java.util.Date
import kotlin.random.Random

@Composable
fun TimeText(date: Date, modifier: Modifier = Modifier, textAlign: TextAlign = TextAlign.Start) {
    var text by remember { mutableStateOf("") }
    val dFormat = DateFormat.getDateInstance()
    val minute = 60L
    val hour = minute*60
    val day = hour*24
    val week = day*7
    val biWeekly = week*2

    // Sets text, returns the least significant amount of time before anything would change.
    fun calculateTime(): Long {
        val timeSince = (Date().time - date.time) / 1000
        var toReturn = -1L
        if (timeSince < minute) {
            // Seconds ago
            text = "$timeSince seconds ago"
            toReturn = 1
        } else if (timeSince < hour) {
            // Minutes ago
            text = "${timeSince/minute} minutes ago"
            toReturn = minute
        } else if (timeSince < day) {
            // Hours ago
            text = "${timeSince/hour} hours ago"
        } else if (timeSince < week) {
            // Days ago
            text = "${timeSince/day} days ago"
        } else if (timeSince < biWeekly) {
            // 1 week ago??
            text = "over 1 week ago"
        } else {
            text = dFormat.format(date)
        }
        return toReturn
    }

    var sleepTime = calculateTime()
    LaunchedEffect(Unit) {
        while (true) {
            if (sleepTime == -1L) break
            delay(sleepTime*1000)
            sleepTime = calculateTime()
        }
    }

    Text(
        text,
        modifier = modifier,
        textAlign = textAlign
    )
}