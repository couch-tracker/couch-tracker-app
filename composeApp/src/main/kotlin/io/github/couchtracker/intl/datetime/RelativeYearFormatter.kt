package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

/**
 * This class formats a year ([Int]) relative to the given [Instant] in a given [TimeZone].
 *
 * Examples:
 * - `this year`
 * - `last year`
 * - `next year`
 * - `6 years ago`
 * - `in 1 year`
 */
class RelativeYearFormatter(
    locale: ULocale,
    style: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    capitalizationContext: DisplayContext = DisplayContext.CAPITALIZATION_NONE,
) : AbstractRelativeLocalFormatter<Int>(locale, style, capitalizationContext) {

    override val absoluteUnit get() = AbsoluteUnit.YEAR
    override val relativeUnit get() = RelativeUnit.YEARS
    override fun Zoned<Instant>.toLocalPart() = toLocalDateTime().year
    override fun Int.unitsUntil(other: Int) = other - this
    override fun startOfNextUnit(value: Int, timeZone: TimeZone): Instant {
        return LocalDate(year = value + 1, month = Month.JANUARY, day = 1).atStartOfDayIn(timeZone)
    }

    fun format(year: Int, now: Zoned<Instant>) = formatLocalValue(year, now)
}
