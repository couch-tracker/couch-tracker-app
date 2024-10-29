package io.github.couchtracker.intl

import androidx.compose.ui.text.intl.Locale
import io.github.couchtracker.intl.datetime.Skeleton
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone

expect fun formatAndList(items: List<String>): String

/**
 * Formats the given [instant] and [timeZone] with the give [skeleton] for [locale].
 *
 * Note: with `kotlinx-datetime` there is no standard way to pass a generic temporal-like object, like in `java.time` APIs.
 * All the "local" parts used are for the given [instant] at the given [timeZone]. For instance, passing the epoch (`0`) with a timezone of
 * `GMT+05:00` will make the hour value be `5 AM`.
 */
expect fun formatDateTimeSkeleton(instant: Instant, timeZone: TimeZone, skeleton: Skeleton, locale: Locale): String
