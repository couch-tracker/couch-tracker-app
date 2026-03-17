package io.github.couchtracker.utils

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Instant

data class Zoned<out T>(val value: T, val timeZone: TimeZone)
data class MaybeZoned<out T>(val value: T, val timeZone: TimeZone?)

fun Zoned<Instant>.toLocalDateTime() = value.toLocalDateTime(timeZone)

fun Clock.zonedNow() = Zoned(
    value = now(),
    timeZone = TimeZone.currentSystemDefault(),
)

operator fun Zoned<Instant>.plus(duration: Duration) = Zoned(value + duration, timeZone)
