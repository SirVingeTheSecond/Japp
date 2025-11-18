package com.japp.composables

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
        return time.dayOfWeek.name.lowercase()
    } else {
        val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yy")
        return time.format(dateTimeFormatter)
    }
}