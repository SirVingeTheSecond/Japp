package com.japp.composables

import android.annotation.SuppressLint
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import java.text.DateFormat
import java.text.DateFormat.getDateInstance
import java.text.SimpleDateFormat
import java.util.Date

@SuppressLint("SimpleDateFormat")
@Composable
fun TimeText(date: Date, modifier: Modifier = Modifier, style: TextStyle = MaterialTheme.typography.bodySmall, textAlign: TextAlign = TextAlign.Start) {
    val time = (Date().time - date.time) / 1000
    var timeText = ""
    val minute = 60
    val hour = minute*60
    val day = hour*24
    val week = day*7
    val biWeekly = week*2

    if (time < minute) {
        timeText = "${time}s ago"
    } else if (time < hour) {
        timeText = "${time/minute}m ago"
    }else if (time < day) {
        timeText = "${time/hour}h ago"
    }else if (time < week) {
        timeText = "${time/day}d ago"
    }else if (time < biWeekly) {
        timeText = "over a week ago"
    }else {
        timeText = SimpleDateFormat("dd/mm/yy").format(date)
    }


    Text(timeText, modifier = modifier.then(Modifier), style = style, textAlign = textAlign)
}