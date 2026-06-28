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
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minusMonth
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaZoneId
import java.time.ZonedDateTime
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant
import kotlin.time.toJavaInstant

class DynamicYearMonthFormatter(
    private val locale: ULocale,
    private val timeZoneSkeleton: TimezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    val yearSkeleton: YearSkeleton = YearSkeleton.NUMERIC,
    val monthSkeleton: MonthSkeleton = MonthSkeleton.WIDE,
) {

    private val relativeYearFormatter = RelativeYearMonthFormatter(
        locale = locale,
        style = relativeDateStyle,
    )

    fun format(yearMonth: MaybeZoned<YearMonth>, now: Zoned<Instant>): TickingValue<String> {
        return chooseThreshold(
            now = now.value,
            thresholdStart = yearMonth.value.minusMonth().firstDay.atStartOfDayIn(now.timeZone) - 1.nanoseconds,
            thresholdEnd = yearMonth.value.plus(2, DateTimeUnit.MONTH).firstDay.atStartOfDayIn(now.timeZone),
            withinThreshold = {
                val relativeYearFormat = relativeYearFormatter.format(yearMonth.value, now)

                if (yearMonth.timeZone != null) {
                    relativeYearFormat.map {
                        combineLiteralDateAndTimePattern(
                            locale = locale,
                            dateFormatStyle = DateFormat.SHORT,
                            literalDate = it,
                            timePattern = { timeZoneSkeleton.value },
                            value = ZonedDateTime.ofInstant(
                                yearMonth.value.firstDay.atStartOfDayIn(yearMonth.timeZone).toJavaInstant(),
                                yearMonth.timeZone.toJavaZoneId(),
                            ),
                        )
                    }
                } else {
                    relativeYearFormat
                }
            },
            outsideThreshold = {
                val tz = yearMonth.timeZone ?: TimeZone.UTC
                TickingValue(
                    value = formatDateTimeSkeleton(
                        instant = yearMonth.value.firstDay.atStartOfDayIn(tz),
                        timeZone = tz,
                        dateTimeSkeleton = DateTimeSkeleton(yearSkeleton, monthSkeleton),
                        timezoneSkeleton = if (yearMonth.timeZone != null) timeZoneSkeleton else null,
                        locale = locale,
                    ),
                    nextTick = null,
                )
            },
        )
    }
}
