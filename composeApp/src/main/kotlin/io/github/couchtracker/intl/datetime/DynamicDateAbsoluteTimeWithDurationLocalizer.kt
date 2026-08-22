package io.github.couchtracker.intl.datetime

import android.content.Context
import android.icu.util.ULocale
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.Zoned
import dev.mmauro.datetimepolyglot.combine
import dev.mmauro.datetimepolyglot.flatMap
import dev.mmauro.datetimepolyglot.localizers.PolyglotReferenceValueLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.DateStyle
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ExperimentalTickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateTimeOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalTimeStyle
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationOptions
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicDateAbsoluteTimeLocalizer
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicDateAbsoluteTimeOptions
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicLocalizer
import dev.mmauro.datetimepolyglot.localizers.dynamic.ExperimentalDynamicLocalizer
import dev.mmauro.datetimepolyglot.localizers.relative.RelativeDateAbsoluteTimeOptions
import dev.mmauro.datetimepolyglot.localizers.relative.RelativeLocalDateOptions
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import io.github.couchtracker.R
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toInstant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.DurationUnit
import kotlin.time.Instant

data class DynamicDateAbsoluteTimeWithDurationOptions(
    val dateTimeOptions: DynamicDateAbsoluteTimeOptions = DEFAULT_DATE_TIME_OPTIONS,
    val durationOptions: DurationOptions = DurationOptions(minUnit = DurationUnit.MINUTES, style = DurationStyle.SHORT),
    val durationDiffRange: ClosedRange<Duration> = -1.days..1.days,
) {
    companion object {
        val DEFAULT_DATE_TIME_OPTIONS = DynamicDateAbsoluteTimeOptions(
            relativeOptions = RelativeDateAbsoluteTimeOptions(
                dateOptions = RelativeLocalDateOptions(
                    useRelativeDayOfWeek = true,
                ),
                joinerStyle = DateStyle.SHORT,
            ),
            absoluteOptions = LocalDateTimeOptions(DateStyle.MEDIUM, LocalTimeStyle.SHORT),
        )
    }
}

/**
 * Class identical to [DynamicDateAbsoluteTimeLocalizer] but adds the relative duration in parentheses if the distance is within
 * [DynamicDateAbsoluteTimeWithDurationOptions.durationDiffRange].
 *
 * Examples:
 * - `Jan 25, 2026, 3:50 PM`
 * - `01/25/2026, 12:00 AM`
 * - `today at 10:30`
 * - `this Monday at 6pm`
 * - `5 days ago at 4:21`
 * - `today at 10:30 (in 3 hours, 5 minutes)`
 * - `yesterday at 9:00 PM (15h 5m ago)`
 *
 * The [context] is needed to retrieve the localization strings for "in x", "x ago", and one for parenthesizing. For this reason, [locale]
 * should always match the locale of the given [context].
 */
@OptIn(ExperimentalTickingDurationLocalizer::class, ExperimentalDynamicLocalizer::class)
data class DynamicDateAbsoluteTimeWithDurationLocalizer(
    private val context: Context,
    private val options: DynamicDateAbsoluteTimeWithDurationOptions = DynamicDateAbsoluteTimeWithDurationOptions(),
    private val locale: ULocale = ULocale.getDefault(),
) : PolyglotReferenceValueLocalizer<LocalDateTime> {

    private val dynamicDateAbsoluteTimeLocalizer = DynamicDateAbsoluteTimeLocalizer(options.dateTimeOptions, locale)
    private val tickingDurationLocalizer = TickingDurationLocalizer(TickingDurationOptions(options.durationOptions, abs = true), locale)

    override fun localize(value: LocalDateTime, reference: Zoned<Instant>): TickingValue<String> {
        val instant = value.toInstant(reference.timeZone)
        val localizer = DynamicLocalizer(
            DynamicLocalizer.Case.Threshold(
                range = (instant + options.durationDiffRange.start)..<(instant + options.durationDiffRange.endInclusive),
                localize = { value, reference ->
                    val localizedDuration = localizeDuration(value, reference)
                    dynamicDateAbsoluteTimeLocalizer.localize(value, reference).combine(localizedDuration) { mainLoc, durationLoc ->
                        context.getString(R.string.parenthesize, mainLoc, durationLoc)
                    }
                },
            ),
            default = DynamicLocalizer.Case.Default(localizer = dynamicDateAbsoluteTimeLocalizer),
        )

        return localizer.localize(value, reference)
    }

    private fun localizeDuration(value: LocalDateTime, reference: Zoned<Instant>): TickingValue<String> {
        val instant = value.toInstant(reference.timeZone)
        val diff = instant - reference.value
        return tickingDurationLocalizer.localize(diff).flatMap {
            if (diff.isNegative()) {
                TickingValue(
                    value = context.getString(R.string.duration_x_ago, it),
                    nextTick = null,
                )
            } else {
                TickingValue(
                    value = context.getString(R.string.duration_in_x, it),
                    nextTick = diff + 1.nanoseconds,
                )
            }
        }
    }
}
