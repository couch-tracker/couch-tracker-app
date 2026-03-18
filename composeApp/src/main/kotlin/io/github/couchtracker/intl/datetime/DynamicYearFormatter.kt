package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.periodUntil
import kotlin.time.Instant

class DynamicYearFormatter(
    locale: ULocale,
    timeZoneSkeleton: TimezoneSkeleton = TimezoneSkeleton.TIMEZONE_ID,
    relativeDateStyle: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    override val absoluteSkeleton: YearSkeleton = YearSkeleton.NUMERIC,
) : AbstractDynamicLocalPeriodFormatter<Int>(
    locale = locale,
    timeZoneSkeleton = timeZoneSkeleton,
) {

    private val relativeDateAbsoluteTimeFormatter = RelativeYearFormatter(
        locale = locale,
        style = relativeDateStyle,
    )

    override val relativeThreshold get() = DatePeriod(years = 1)
    override fun Int.localToInstant(timeZone: TimeZone) = LocalDate(year = this, Month.JANUARY, 1).atStartOfDayIn(timeZone)
    override fun FormatContext<Int, DateTimePeriod>.formatRelative() = relativeDateAbsoluteTimeFormatter.format(localValue.value, now)
    override fun calculateDiff(valueInstant: Instant, now: Zoned<Instant>, timeZone: TimeZone) =
        now.value.periodUntil(valueInstant, now.timeZone)

    override fun LocalDate.startOfPeriod() = LocalDate(year = this.year, month = Month.JANUARY, day = 1)

    fun format(year: MaybeZoned<Int>, now: Zoned<Instant>) = formatLocalValue(year, now)
}
