package com.japp.utils

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date

object DateTimeHelper {

    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    private val shortDateFormatter = DateTimeFormatter.ofPattern("MMM d")
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a")
    private val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")

    /**
     * Parse a timestamp string (ms) to LocalDateTime.
     */
    fun parseTimestamp(timestamp: String?): LocalDateTime {
        if (timestamp.isNullOrBlank()) return LocalDateTime.now()

        return try {
            val millis = timestamp.toLongOrNull()
            if (millis != null) {
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(millis),
                    ZoneId.systemDefault()
                )
            } else {
                LocalDateTime.parse(timestamp, DateTimeFormatter.ISO_DATE_TIME)
            }
        } catch (_: Exception) {
            LocalDateTime.now()
        }
    }

    /**
     * Parse a Java Date to LocalDateTime.
     */
    fun fromDate(date: Date): LocalDateTime {
        return LocalDateTime.ofInstant(
            Instant.ofEpochMilli(date.time),
            ZoneId.systemDefault()
        )
    }

    /**
     * Format to relative time (such as "Just now", "5m ago", "2h ago", "Yesterday", "Dec 13, 2024")
     */
    fun formatRelative(timestamp: String?): String {
        return formatRelative(parseTimestamp(timestamp))
    }

    /**
     * Format LocalDateTime to relative time.
     */
    fun formatRelative(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()

        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)

        return when {
            minutes < 0 -> "Just now"
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 && dateTime.toLocalDate() == now.toLocalDate() -> "${hours}h ago"
            days == 0L && dateTime.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
            days == 1L -> "Yesterday"
            days < 7 -> "${days}d ago"
            dateTime.year == now.year -> dateTime.format(shortDateFormatter)
            else -> dateTime.format(dateFormatter)
        }
    }

    /**
     * Format Date to relative time
     */
    fun formatRelative(date: Date): String {
        return formatRelative(fromDate(date))
    }

    /**
     * Format to short relative (such as "now", "5m", "2h", "Yesterday", "Mon", "Dec 13")
     */
    fun formatShortRelative(timestamp: String?): String {
        return formatShortRelative(parseTimestamp(timestamp))
    }

    /**
     * Format LocalDateTime to short relative time.
     */
    fun formatShortRelative(dateTime: LocalDateTime): String {
        val now = LocalDateTime.now()

        val minutes = ChronoUnit.MINUTES.between(dateTime, now)
        val hours = ChronoUnit.HOURS.between(dateTime, now)
        val days = ChronoUnit.DAYS.between(dateTime, now)
        val weeks = ChronoUnit.WEEKS.between(dateTime, now)

        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            hours < 24 && dateTime.toLocalDate() == now.toLocalDate() -> "${hours}h"
            days == 0L && dateTime.toLocalDate() == now.toLocalDate().minusDays(1) -> "Yesterday"
            days == 1L -> "Yesterday"
            weeks == 0L -> dateTime.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            dateTime.year == now.year -> dateTime.format(shortDateFormatter)
            else -> dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yy"))
        }
    }

    /**
     * Format to full date (such as "Dec 13, 2024")
     */
    fun formatDate(timestamp: String?): String {
        return parseTimestamp(timestamp).format(dateFormatter)
    }

    /**
     * Format to date and time (such as "Dec 13, 2024 at 3:30 PM")
     */
    fun formatDateTime(timestamp: String?): String {
        return parseTimestamp(timestamp).format(dateTimeFormatter)
    }

    /**
     * Format to time only (such as "3:30 PM")
     */
    fun formatTime(timestamp: String?): String {
        return parseTimestamp(timestamp).format(timeFormatter)
    }

    /**
     * Format for chat messages: time if today, short date if this year, full date otherwise.
     */
    fun formatMessageTime(timestamp: String?): String {
        val dateTime = parseTimestamp(timestamp)
        val now = LocalDateTime.now()

        return when {
            dateTime.toLocalDate() == now.toLocalDate() -> dateTime.format(timeFormatter)
            dateTime.year == now.year -> dateTime.format(shortDateFormatter)
            else -> dateTime.format(dateFormatter)
        }
    }
}
