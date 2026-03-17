package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * This class formats a [LocalDate] relative to the given [Instant] in a given [TimeZone].
 *
 * Depending on the days difference, it can use three formats:
 * - specific words to like today, tomorrow and yesterday
 * - this/next day-of-week, limited to 7 days in the future
 * - x days ago/in x days
 *
 * Examples:
 * - `today`
 * - `yesterday`
 * - `6 days ago`
 * - `this Tuesday`
 * - `next Saturday`
 * - `in 10 days`
 */
data class RelativeLocalDateFormatter(
    private val locale: ULocale,
    private val style: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val capitalizationContext: DisplayContext = DisplayContext.CAPITALIZATION_NONE,
) : AbstractRelativeLocalFormatter<LocalDate>(locale, style, capitalizationContext) {

    override val absoluteUnit get() = AbsoluteUnit.DAY
    override val relativeUnit get() = RelativeUnit.DAYS
    override fun Zoned<Instant>.toLocalPart() = toLocalDateTime().date
    override fun LocalDate.unitsUntil(other: LocalDate) = daysUntil(other)
    override fun startOfNextUnit(value: LocalDate, timeZone: TimeZone) = value.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone)

    override fun FormatContext<LocalDate>.formats() = sequence {
        formatDirection()?.let { yield(it) }
        formatDayOfWeek()?.let { yield(it) }
        yield(formatRelativeUnit())
    }

    private fun FormatContext<LocalDate>.formatDayOfWeek(): TickingValue<String>? {
        return if (diff in 1..DayOfWeek.entries.size) {
            val firstDoWIndex = Calendar.getInstance(locale).firstDayOfWeek
            val firstDoW = DayOfWeek.entries.single {
                it.ordinal == (firstDoWIndex - 2).mod(DayOfWeek.entries.size)
            }
            val nextFirstDoW = List(DayOfWeek.entries.size) { nowLocal.plus(DatePeriod(days = it + 1)) }.first { it.dayOfWeek == firstDoW }

            val direction = when {
                localValue < nextFirstDoW -> Direction.THIS
                else -> Direction.NEXT
            }
            relativeDateTimeFormatter.format(direction, localValue.dayOfWeek.toIcuAbsoluteUnit())?.let {
                // Calculating a nextTick here is tricky because "this/next <day-of-week>" can be valid for multiple days, until either the
                // direction changes (this becomes next) or a specific word is used (e.g. tomorrow), whose existence depends on the locale.
                // Hence, we take a cheap shortcut here by checking the next day format to see if it would change
                val nextDayFormat = format(localValue, Zoned(now.value + nextUnitTick, now.timeZone))
                val nextTick = if (nextDayFormat.value != it) {
                    nextUnitTick
                } else {
                    nextUnitTick + (nextDayFormat.nextTick ?: Duration.ZERO)
                }
                TickingValue(it, nextTick = nextTick)
            }
        } else {
            null
        }
    }

    fun format(localDate: LocalDate, now: Zoned<Instant>) = formatLocalValue(localDate, now)
}

private fun DayOfWeek.toIcuAbsoluteUnit(): AbsoluteUnit = when (this) {
    DayOfWeek.MONDAY -> AbsoluteUnit.MONDAY
    DayOfWeek.TUESDAY -> AbsoluteUnit.TUESDAY
    DayOfWeek.WEDNESDAY -> AbsoluteUnit.WEDNESDAY
    DayOfWeek.THURSDAY -> AbsoluteUnit.THURSDAY
    DayOfWeek.FRIDAY -> AbsoluteUnit.FRIDAY
    DayOfWeek.SATURDAY -> AbsoluteUnit.SATURDAY
    DayOfWeek.SUNDAY -> AbsoluteUnit.SUNDAY
}
