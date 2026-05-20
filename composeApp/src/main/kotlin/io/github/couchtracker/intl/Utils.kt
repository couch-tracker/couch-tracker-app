package io.github.couchtracker.intl

import android.icu.text.ListFormatter
import com.ibm.icu.impl.SimpleFormatterImpl
import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.DateTimePatternGenerator
import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.ULocale
import io.github.couchtracker.intl.datetime.DateTimeSkeleton
import io.github.couchtracker.intl.datetime.TimezoneSkeleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaZoneId
import java.time.ZonedDateTime
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
fun formatDateTimeSkeleton(
    instant: Instant,
    timeZone: TimeZone,
    dateTimeSkeleton: DateTimeSkeleton,
    timezoneSkeleton: TimezoneSkeleton?,
    locale: ULocale,
): String {
    val zonedDateTime = instant.toJavaInstant().atZone(timeZone.toJavaZoneId())

    if (dateTimeSkeleton.time == null && timezoneSkeleton != null) {
        // In this case, we need to work around a bug in ICU, so we need to combine the date/time format and the timezone separately

        val dateTimePattern = DateFormat.getInstanceForSkeleton(dateTimeSkeleton.value, locale)
        val dateTimeFormat = dateTimePattern.format(zonedDateTime)

        return combineLiteralDateAndTimePattern(
            locale = locale,
            dateFormatStyle = DateFormat.SHORT,
            literalDate = dateTimeFormat,
            timePattern = { timezoneSkeleton.value },
            value = zonedDateTime,
        )
    }

    val skeleton = listOfNotNull(dateTimeSkeleton, timezoneSkeleton).joinToString(separator = " ") { it.value }
    val pattern = DateFormat.getInstanceForSkeleton(skeleton, locale)
    return pattern.format(zonedDateTime)
}

fun combineLiteralDateAndTimePattern(
    locale: ULocale,
    dateFormatStyle: Int,
    literalDate: String,
    timePattern: DateTimePatternGenerator.() -> String,
    value: ZonedDateTime,
): String {
    val generator = DateTimePatternGenerator.getInstance(locale)
    val dateTimePattern = generator.getDateTimeFormat(dateFormatStyle)
    val pattern = SimpleFormatterImpl.formatRawPattern(
        dateTimePattern,
        2, // min
        2, // max
        timePattern(generator),
        "'${literalDate.replace("'", "''")}'",
    )

    return SimpleDateFormat(pattern, locale).format(value)
}
