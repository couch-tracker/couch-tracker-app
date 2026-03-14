package io.github.couchtracker.intl.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.ULocale
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_FORMAT_WIDTH
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_MAX_UNITS
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_MIN_UNIT
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_OMIT_ZEROS
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.remainderUntilNextUnitBoundary
import io.github.couchtracker.utils.rememberTickingValue
import io.github.couchtracker.utils.toULocale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

class RelativeDurationFormatter(
    locale: ULocale,
    formatWidth: FormatWidth = DEFAULT_FORMAT_WIDTH,
    private val omitZeros: Boolean = DEFAULT_OMIT_ZEROS,
    private val minUnit: DurationUnit = DEFAULT_MIN_UNIT,
    private val maxUnits: Int = DEFAULT_MAX_UNITS,
) {

    private val durationFormatter = DurationFormatter(
        locale = locale,
        formatWidth = formatWidth,
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
    )

    fun format(duration: Duration): TickingValue<String> {
        val absDuration = duration.absoluteValue
        return TickingValue(
            value = durationFormatter.format(absDuration),
            nextTick = durationFormatter.units(absDuration).minOf { duration.remainderUntilNextUnitBoundary(it) },
        )
    }
}

/**
 * @param omitZeros whether to avoid printing units that have a value of zero. At least one unit (the smallest) will always be printed.
 * @param minUnit the smallest unit that should be printed. All smaller units are discarded.
 * @param maxUnits maximum number of units that should be printed. Smaller units are discarded.
 * @param formatWidth the width to use for the format. See [MeasureFormat.getInstance].
 * @param locale the locale to format with. If `null`, uses the system default
 */
@Composable
fun rememberRelativeDurationFormatter(
    locale: ULocale? = null,
    formatWidth: FormatWidth = DEFAULT_FORMAT_WIDTH,
    omitZeros: Boolean = DEFAULT_OMIT_ZEROS,
    minUnit: DurationUnit = DEFAULT_MIN_UNIT,
    maxUnits: Int = DEFAULT_MAX_UNITS,
): RelativeDurationFormatter {
    val locale = locale ?: LocalConfiguration.currentFirstLocale.toULocale()

    return remember(locale, formatWidth, omitZeros, minUnit, maxUnits) {
        RelativeDurationFormatter(
            locale = locale,
            formatWidth = formatWidth,
            omitZeros = omitZeros,
            minUnit = minUnit,
            maxUnits = maxUnits,
        )
    }
}

/**
 * Utility to format a duration relative to a given [instant].
 *
 * The format will automatically run only when necessary (e.g. every second if it's displaying the seconds unit).
 *
 * @param instant the instant to which the relative time must be computed
 * @param formatter the formatter to use to format the relative time. See [rememberRelativeDurationFormatter]. Every time the formatter
 * changes, the duration is recomputed the reformatted.
 * @param format lambda that, if provided, allows to override the standard behavior (e.g. add an exception, wrap the returned format, etc.).
 * It receives a [RelativeDurationFormatter] as scope and the current relative [Duration] as input, and must calculate the wanted [Text].
 * @return the formatted [String] as a result of [format]
 */
@Composable
fun rememberRelativeDurationText(
    instant: Instant,
    vararg keys: Any?,
    formatter: RelativeDurationFormatter = rememberRelativeDurationFormatter(),
    format: RelativeDurationFormatter.(Duration) -> TickingValue<Text> = { format(it).map(Text::Literal) },
): String {
    return rememberTickingValue(formatter, instant, *keys) {
        val relative = instant - Clock.System.now()

        format(formatter, relative)
    }.string()
}
