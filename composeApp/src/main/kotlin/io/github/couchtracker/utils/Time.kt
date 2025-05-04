package io.github.couchtracker.utils

import kotlinx.datetime.LocalDateTime

fun LocalDateTime.roundToSeconds() = LocalDateTime(
    year = year,
    month = month,
    dayOfMonth = dayOfMonth,
    hour = hour,
    minute = minute,
    second = second,
)
