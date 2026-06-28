package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.map
import io.github.couchtracker.intl.combineLiteralDateAndTimePattern
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.Zoned
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaZoneId
import kotlin.time.Instant

/**
 * Class that formats the date in a relative way (see [RelativeLocalDateFormatter]), while the time always formatted in an absolute way.
 *
 * The [timeZoneSkeleton] is used only if the given date time value belongs to a timezone and this timezone is different from the one of
 * now.
 *
 * Examples:
 * - `today at 10:30`
 * - `today at 10:30 Central European Standard Time`
 * - `this Monday at 6pm`
 * - `5 days ago at 4:21`
 * - `5 days ago at 4:21 Europe/Rome`
 */
class RelativeDateAbsoluteTimeFormatter(
    private val locale: ULocale,
    private val timeSkeleton: TimeSkeleton = TimeSkeleton.MINUTES,
    private val timeZoneSkeleton: TimezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val dateFormatStyle: Int = DateFormat.FULL,
) {

    init {
        require(dateFormatStyle in DateFormat.FULL..DateFormat.SHORT) { "Invalid date format style: $dateFormatStyle" }
    }

    private val relativeLocalDateFormatter = RelativeLocalDateFormatter(locale, style = relativeDateStyle)

    fun format(dateTime: MaybeZoned<LocalDateTime>, now: Zoned<Instant>): TickingValue<String> {
        val dateFormat = relativeLocalDateFormatter.format(dateTime.value.date, now)

        val skeletons = listOfNotNull(
            timeSkeleton,
            timeZoneSkeleton.takeIf { dateTime.timeZone != null && dateTime.timeZone != now.timeZone },
        )

        return dateFormat.map {
            combineLiteralDateAndTimePattern(
                locale = locale,
                dateFormatStyle = dateFormatStyle,
                literalDate = it,
                timePattern = { getBestPattern(skeletons.sum().value) },
                value = dateTime.value.toJavaLocalDateTime().atZone((dateTime.timeZone ?: now.timeZone).toJavaZoneId()),
            )
        }
    }
}
