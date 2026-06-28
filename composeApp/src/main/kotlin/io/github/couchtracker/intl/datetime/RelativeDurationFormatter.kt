package io.github.couchtracker.intl.datetime

import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ExperimentalTickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationOptions
import dev.mmauro.datetimepolyglot.map
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.rememberTickingValue
import io.github.couchtracker.utils.toAndroidULocale
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant


@OptIn(ExperimentalTickingDurationLocalizer::class)
@Composable
fun rememberTickingDurationFormatter(
    locale: ULocale? = null,
    options: TickingDurationOptions = TickingDurationOptions(
        abs = true,
        durationOptions = DurationOptions(
            style = DurationStyle.SHORT,
            minUnit = DurationUnit.MINUTES,
        ),
    ),
): TickingDurationLocalizer {
    val locale = locale ?: LocalConfiguration.currentFirstLocale.toAndroidULocale()

    return remember(locale, options) {
        TickingDurationLocalizer(
            locale = locale,
            options = options,
        )
    }
}

/**
 * Utility to format a duration relative to a given [instant].
 *
 * The format will automatically run only when necessary (e.g. every second if it's displaying the seconds unit).
 *
 * @param instant the instant to which the relative time must be computed
 * @param formatter the formatter to use to format the relative time. See [rememberTickingDurationFormatter]. Every time the formatter
 * changes, the duration is recomputed the reformatted.
 * @param format lambda that, if provided, allows to override the standard behavior (e.g. add an exception, wrap the returned format, etc.).
 * It receives a [TickingDurationLocalizer] as scope and the current relative [Duration] as input, and must calculate the wanted [Text].
 * @return the formatted [String] as a result of [format]
 */
@OptIn(ExperimentalTickingDurationLocalizer::class)
@Composable
fun rememberTickingDurationText(
    instant: Instant,
    vararg keys: Any?,
    formatter: TickingDurationLocalizer = rememberTickingDurationFormatter(),
    format: TickingDurationLocalizer.(Duration) -> TickingValue<Text> = { localize(it).map(Text::Literal) },
): String {
    return rememberTickingValue(formatter, instant, *keys) {
        val relative = instant - Clock.System.now()

        format(formatter, relative)
    }.string()
}
