package io.github.couchtracker.intl

import android.icu.text.ListFormatter
import android.text.format.DateFormat
import androidx.compose.ui.text.intl.Locale
import io.github.couchtracker.intl.datetime.Skeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter
import kotlin.time.Instant
import kotlin.time.toJavaInstant

suspend fun formatAndList(items: List<String>): String {
    return withContext(Dispatchers.Default) {
        ListFormatter.getInstance().format(items)
    }
}

/**
 * Formats the given [instant] and [timeZone] with the give [skeleton] for [locale].
 *
 * Note: with `kotlinx-datetime` there is no standard way to pass a generic temporal-like object, like in `java.time` APIs.
 * All the "local" parts used are for the given [instant] at the given [timeZone]. For instance, passing the epoch (`0`) with a timezone of
 * `GMT+05:00` will make the hour value be `5 AM`.
 */
fun formatDateTimeSkeleton(instant: Instant, timeZone: TimeZone, skeleton: Skeleton, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale.platformLocale, skeleton.value)
    val temporal = instant.toJavaInstant().atZone(timeZone.toJavaZoneId())
    return DateTimeFormatter.ofPattern(pattern, locale.platformLocale).format(temporal)
}
