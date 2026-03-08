package io.github.couchtracker.intl.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.ULocale
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
    private val omitZeros: Boolean = true,
    private val minUnit: DurationUnit = DurationUnit.MINUTES,
    private val maxUnits: Int = 2,
    private val measureFormat: MeasureFormat,
) {

    private val durationFormatter = DurationFormatter(
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        measureFormat = measureFormat,
    )

    fun format(duration: Duration): TickingValue<String> {
        return TickingValue(
            value = durationFormatter.format(duration),
            nextTick = durationFormatter.units(duration).minOf { duration.remainderUntilNextUnitBoundary(it) },
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
    omitZeros: Boolean = true,
    minUnit: DurationUnit = DurationUnit.MINUTES,
    maxUnits: Int = 2,
    formatWidth: MeasureFormat.FormatWidth = MeasureFormat.FormatWidth.SHORT,
    locale: ULocale? = null,
): RelativeDurationFormatter {
    val locale = locale ?: LocalConfiguration.currentFirstLocale.toULocale()
    val measureFormat = remember(locale, formatWidth) { MeasureFormat.getInstance(locale, formatWidth) }

    return remember(omitZeros, minUnit, maxUnits, measureFormat) {
        RelativeDurationFormatter(
            omitZeros = omitZeros,
            minUnit = minUnit,
            maxUnits = maxUnits,
            measureFormat = measureFormat,
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
