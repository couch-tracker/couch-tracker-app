package io.github.couchtracker.intl

import android.icu.text.ListFormatter
import android.text.format.DateFormat
import androidx.compose.ui.text.intl.Locale
import io.github.couchtracker.intl.datetime.Skeleton
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaInstant
import kotlinx.datetime.toJavaZoneId
import java.time.format.DateTimeFormatter

actual fun formatAndList(items: List<String>): String {
    return ListFormatter.getInstance().format(items)
}

actual fun formatDateTimeSkeleton(instant: Instant, timeZone: TimeZone, skeleton: Skeleton, locale: Locale): String {
    val pattern = DateFormat.getBestDateTimePattern(locale.platformLocale, skeleton.value)
    val temporal = instant.toJavaInstant().atZone(timeZone.toJavaZoneId())
    return DateTimeFormatter.ofPattern(pattern, locale.platformLocale).format(temporal)
}
