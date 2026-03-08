package io.github.couchtracker.utils

import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.TimeUnit
import kotlinx.datetime.DateTimePeriod

fun DateTimePeriod.isAnyComponentNegative(): Boolean {
    return days < 0 || months < 0 || years < 0 || hours < 0 || minutes < 0 || seconds < 0 || nanoseconds < 0
}

enum class DateTimePeriodUnit(val imbIcuUnit: MeasureUnit) {
    NANOSECONDS(TimeUnit.NANOSECOND),
    MICROSECONDS(TimeUnit.MICROSECOND),
    MILLISECONDS(TimeUnit.MILLISECOND),
    SECONDS(TimeUnit.SECOND),
    MINUTES(TimeUnit.MINUTE),
    HOURS(TimeUnit.HOUR),
    DAYS(TimeUnit.DAY),
    MONTHS(TimeUnit.MONTH),
    YEARS(TimeUnit.YEAR),
}

/**
 * Returns the part of this [DateTimePeriod] of the given [unit].
 *
 * For example:
 * - `1h30m`, `MINUTES` -> `30`
 * - `30m`, `HOURS` -> `0`
 * - `-50m44s`, `SECONDS` -> `-44`
 */
@Suppress("MagicNumber")
fun DateTimePeriod.unitPart(unit: DateTimePeriodUnit) = when (unit) {
    DateTimePeriodUnit.NANOSECONDS -> (nanoseconds % 1_000).toLong()
    DateTimePeriodUnit.MICROSECONDS -> ((nanoseconds / 1000) % 1000).toLong()
    DateTimePeriodUnit.MILLISECONDS -> nanoseconds / 1_000_000L
    DateTimePeriodUnit.SECONDS -> seconds.toLong()
    DateTimePeriodUnit.MINUTES -> minutes.toLong()
    DateTimePeriodUnit.HOURS -> hours.toLong()
    DateTimePeriodUnit.DAYS -> days.toLong()
    DateTimePeriodUnit.MONTHS -> months.toLong()
    DateTimePeriodUnit.YEARS -> years.toLong()
}
