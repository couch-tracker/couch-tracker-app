package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.plusMonth
import kotlinx.datetime.yearMonth
import kotlin.time.Instant

/**
 * This class formats a [YearMonth] relative to the given [Instant] in a given [TimeZone].
 *
 * Examples:
 * - `this month`
 * - `last month`
 * - `next month`
 * - `4 months ago`
 * - `in 34 months`
 */
class RelativeYearMonthFormatter(
    locale: ULocale,
    style: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    capitalizationContext: DisplayContext = DisplayContext.CAPITALIZATION_NONE,
) : AbstractRelativeLocalFormatter<YearMonth>(locale, style, capitalizationContext) {

    override val absoluteUnit get() = AbsoluteUnit.MONTH
    override val relativeUnit get() = RelativeUnit.MONTHS
    override fun Zoned<Instant>.toLocalPart() = toLocalDateTime().date.yearMonth
    override fun YearMonth.unitsUntil(other: YearMonth) = this.monthsUntil(other)
    override fun startOfNextUnit(value: YearMonth, timeZone: TimeZone): Instant {
        return value.plusMonth().firstDay.atStartOfDayIn(timeZone)
    }

    fun format(yearMonth: YearMonth, now: Zoned<Instant>) = formatLocalValue(yearMonth, now)
}
