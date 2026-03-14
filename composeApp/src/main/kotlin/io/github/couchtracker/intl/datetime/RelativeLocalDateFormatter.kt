package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.NumberFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.util.Calendar
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.math.absoluteValue
import kotlin.time.Instant

private const val TWO_DAYS_AGO = -2
private const val ONE_DAY_AGO = -1
private const val TODAY = 0
private const val IN_ONE_DAY = 1
private const val IN_TWO_DAYS = 2

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
    private val numberFormat: NumberFormat? = null,
    private val style: RelativeDateTimeFormatter.Style = RelativeDateTimeFormatter.Style.LONG,
    private val capitalizationContext: DisplayContext = DisplayContext.CAPITALIZATION_NONE,
) {

    private val relativeDateTimeFormatter = RelativeDateTimeFormatter.getInstance(locale, numberFormat, style, capitalizationContext)

    fun format(date: LocalDate, now: Instant, tz: TimeZone): TickingValue<String> {
        val nowInDate = now.toLocalDateTime(tz).date
        val days = nowInDate.daysUntil(date)

        val absoluteFormattedDay = when (days) {
            in TWO_DAYS_AGO..IN_TWO_DAYS -> {
                val direction = when (days) {
                    TWO_DAYS_AGO -> Direction.LAST_2
                    ONE_DAY_AGO -> Direction.LAST
                    TODAY -> Direction.THIS
                    IN_ONE_DAY -> Direction.NEXT
                    IN_TWO_DAYS -> Direction.NEXT_2
                    else -> error("invalid days amount $days")
                }
                // This returns null if the locale doesn't have a way to say this
                relativeDateTimeFormatter.format(direction, AbsoluteUnit.DAY)
            }
            else -> null
        }

        val thisNextDow = if (absoluteFormattedDay == null && days in 1..DayOfWeek.entries.size) {
            val firstDoWIndex = Calendar.getInstance(locale).firstDayOfWeek
            val firstDoW = DayOfWeek.entries.single {
                it.ordinal == (firstDoWIndex - 2).mod(DayOfWeek.entries.size)
            }
            val nextFirstDoW = List(DayOfWeek.entries.size) { nowInDate.plus(DatePeriod(days = it + 1)) }.first { it.dayOfWeek == firstDoW }

            val direction = when {
                date < nextFirstDoW -> Direction.THIS
                else -> Direction.NEXT
            }
            relativeDateTimeFormatter.format(direction, date.dayOfWeek.toIcuAbsoluteUnit())
        } else {
            null
        }

        return TickingValue(
            value = absoluteFormattedDay ?: thisNextDow ?: relativeDateTimeFormatter.format(
                days.absoluteValue.toDouble(),
                if (days < 0) Direction.LAST else Direction.NEXT,
                RelativeUnit.DAYS,
            ),
            nextTick = (nowInDate + DatePeriod(days = 1)).atStartOfDayIn(tz) - now,
        )
    }
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
