package io.github.couchtracker.intl.datetime

import com.ibm.icu.impl.SimpleFormatterImpl
import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.DateTimePatternGenerator
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.SimpleDateFormat
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toJavaZoneId
import kotlin.time.Instant

/**
 * Class that formats the date in a relative way (see [RelativeLocalDateFormatter]), while the time always formatted in an absolute way.
 *
 * Examples:
 * - `today at 10:30`
 * - `this Monday at 6pm`
 * - `5 days ago at 4:21`
 */
class RelativeDateAbsoluteTimeFormatter(
    private val locale: ULocale,
    private val timeSkeleton: TimeSkeleton = TimeSkeleton.MINUTES,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val dateFormatStyle: Int = DateFormat.FULL,
) {

    init {
        require(dateFormatStyle in DateFormat.FULL..DateFormat.SHORT) { "Invalid date format style: $dateFormatStyle" }
    }

    private val relativeLocalDateFormatter = RelativeLocalDateFormatter(locale, style = relativeDateStyle)

    fun format(dateTime: LocalDateTime, now: Zoned<Instant>): TickingValue<String> {
        val dateFormat = relativeLocalDateFormatter.format(dateTime.date, now)

        val generator = DateTimePatternGenerator.getInstance(locale)
        val dateTimePattern = generator.getDateTimeFormat(dateFormatStyle)
        val pattern = SimpleFormatterImpl.formatRawPattern(
            dateTimePattern,
            2, // min
            2, // max
            generator.getBestPattern(timeSkeleton.value),
            "'${dateFormat.value.replace("'", "''")}'",
        )

        return TickingValue(
            value = SimpleDateFormat(pattern, locale).format(dateTime.toJavaLocalDateTime().atZone(now.timeZone.toJavaZoneId())),
            nextTick = dateFormat.nextTick,
        )
    }
}
