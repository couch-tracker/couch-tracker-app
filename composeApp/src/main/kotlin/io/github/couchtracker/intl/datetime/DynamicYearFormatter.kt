package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.map
import io.github.couchtracker.intl.combineLiteralDateAndTimePattern
import io.github.couchtracker.intl.formatDateTimeSkeleton
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.Zoned
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toJavaZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class DynamicYearFormatter(
    private val locale: ULocale,
    private val timeZoneSkeleton: TimezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    val absoluteSkeleton: YearSkeleton = YearSkeleton.NUMERIC,
) {

    private val relativeYearFormatter = RelativeYearFormatter(
        locale = locale,
        style = relativeDateStyle,
    )

    fun format(year: MaybeZoned<Int>, now: Zoned<Instant>): TickingValue<String> {
        return chooseThreshold(
            now = now.value,
            thresholdStart = LocalDate(year = year.value - 1, month = Month.JANUARY, day = 1).atStartOfDayIn(now.timeZone) - 1.nanoseconds,
            thresholdEnd = LocalDate(year = year.value + 2, month = Month.JANUARY, day = 1).atStartOfDayIn(now.timeZone),
            withinThreshold = {
                val relativeYearFormat = relativeYearFormatter.format(year.value, now)

                if (year.timeZone != null) {
                    relativeYearFormat.map {
                        combineLiteralDateAndTimePattern(
                            locale = locale,
                            dateFormatStyle = DateFormat.SHORT,
                            literalDate = it,
                            timePattern = { timeZoneSkeleton.value },
                            value = ZonedDateTime.ofInstant(
                                LocalDate(year.value, Month.JANUARY, 1).atStartOfDayIn(year.timeZone).toJavaInstant(),
                                year.timeZone.toJavaZoneId(),
                            ),
                        )
                    }
                } else {
                    relativeYearFormat
                }
            },
            outsideThreshold = {
                val tz = year.timeZone ?: TimeZone.UTC
                TickingValue(
                    value = formatDateTimeSkeleton(
                        instant = LocalDate(year.value, month = Month.JANUARY, day = 1).atStartOfDayIn(tz),
                        timeZone = tz,
                        dateTimeSkeleton = DateTimeSkeleton(absoluteSkeleton),
                        timezoneSkeleton = if (year.timeZone != null) timeZoneSkeleton else null,
                        locale = locale,
                    ),
                    nextTick = null,
                )
            },
        )
    }
}
