package io.github.couchtracker.intl.datetime

import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.toLocalDateTime
import io.github.couchtracker.utils.withNextTickAtMost
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.nanoseconds

abstract class AbstractDynamicLocalPeriodFormatter<T>(
    locale: ULocale,
    timeZoneSkeleton: TimezoneSkeleton,
) : AbstractDynamicLocalFormatter<T, DateTimePeriod>(
    locale = locale,
    timeZoneSkeleton = timeZoneSkeleton,
) {

    protected abstract fun LocalDate.startOfPeriod(): LocalDate

    final override fun FormatContext<T, DateTimePeriod>.chooseThreshold(
        threshold: DateTimePeriod,
        withinThreshold: () -> TickingValue<String>,
        outsideThreshold: () -> TickingValue<String>,
    ): TickingValue<String> {
        val nowLocal = now.toLocalDateTime()
        return if (PERIOD_COMPARATOR.compare(diff.absoluteValue, threshold) < 0) {
            val willGoOutsideThresholdOrSwitchSignAt = if (diff.isNegative()) {
                (nowLocal + threshold + diff)
            } else {
                (nowLocal + diff + DateTimePeriod(nanoseconds = 1))
            }

            withinThreshold().withNextTickAtMost(willGoOutsideThresholdOrSwitchSignAt.toInstant(now.timeZone) - now.value)
        } else {
            val willGoWithinThresholdIn = if (diff.isNegative()) {
                null
            } else {
                ((nowLocal + diff - threshold + DateTimePeriod(nanoseconds = 1)).toInstant(now.timeZone)) - now.value
            }
            outsideThreshold().withNextTickAtMost(willGoWithinThresholdIn)
        }
    }
}

private val PERIOD_COMPARATOR = compareBy<DateTimePeriod> { it.years }
    .thenBy { it.months }
    .thenBy { it.days }
    .thenBy { it.hours }
    .thenBy { it.minutes }
    .thenBy { it.seconds }
    .thenBy { it.nanoseconds }

private fun DateTimePeriod.isNegative() = years < 0 || months < 0 || days < 0 || hours < 0 || minutes < 0 || seconds < 0 || nanoseconds < 0

private val DateTimePeriod.absoluteValue
    get() = DateTimePeriod(
        years = years.absoluteValue,
        months = months.absoluteValue,
        days = days.absoluteValue,
        hours = hours.absoluteValue,
        minutes = minutes.absoluteValue,
        seconds = seconds.absoluteValue,
        nanoseconds = nanoseconds.absoluteValue.toLong(),
    )

private operator fun LocalDateTime.plus(period: DateTimePeriod): LocalDateTime {
    // TODO comment
    return this.toJavaLocalDateTime()
        .plus(period.years.toLong(), ChronoUnit.YEARS)
        .plus(period.months.toLong(), ChronoUnit.MONTHS)
        .plus(period.days.toLong(), ChronoUnit.DAYS)
        .plus(period.hours.toLong(), ChronoUnit.HOURS)
        .plus(period.minutes.toLong(), ChronoUnit.MINUTES)
        .plus(period.seconds.toLong(), ChronoUnit.SECONDS)
        .plus(period.nanoseconds.toLong(), ChronoUnit.NANOS)
        .toKotlinLocalDateTime()
}

private operator fun LocalDateTime.minus(period: DateTimePeriod): LocalDateTime {
    // TODO comment
    return this.toJavaLocalDateTime()
        .minus(period.years.toLong(), ChronoUnit.YEARS)
        .minus(period.months.toLong(), ChronoUnit.MONTHS)
        .minus(period.days.toLong(), ChronoUnit.DAYS)
        .minus(period.hours.toLong(), ChronoUnit.HOURS)
        .minus(period.minutes.toLong(), ChronoUnit.MINUTES)
        .minus(period.seconds.toLong(), ChronoUnit.SECONDS)
        .minus(period.nanoseconds.toLong(), ChronoUnit.NANOS)
        .toKotlinLocalDateTime()
}
