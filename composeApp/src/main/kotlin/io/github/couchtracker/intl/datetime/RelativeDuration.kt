package io.github.couchtracker.intl.datetime

import androidx.compose.runtime.Composable
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ExperimentalTickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.TickingDurationOptions
import dev.mmauro.datetimepolyglot.map
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.rememberTickingValue
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.Instant

/**
 * Utility to localize a duration relative to a given [instant].
 *
 * The localization will automatically run only when necessary (e.g. every second if it's displaying the seconds unit).
 *
 * @param instant the instant to which the relative time must be computed
 * @param options the options to pass to [TickingDurationLocalizer]
 * @param localize lambda that, if provided, allows to override the standard behavior (e.g. add an exception, wrap the returned
 * localization, etc.).
 * It receives a [TickingDurationLocalizer] as scope and the current relative [Duration] as input, and must calculate the wanted [Text].
 * @return the formatted [String] as a result of [localize]
 */
@OptIn(ExperimentalTickingDurationLocalizer::class)
@Composable
fun rememberTickingDurationText(
    instant: Instant,
    vararg keys: Any?,
    options: TickingDurationOptions = TickingDurationOptions(
        abs = true,
        durationOptions = DurationOptions(
            style = DurationStyle.SHORT,
            minUnit = DurationUnit.MINUTES,
        ),
    ),
    localize: TickingDurationLocalizer.(Duration) -> TickingValue<Text> = { localize(it).map(Text::Literal) },
): String {
    // TODO use Flows?
    val localizer = rememberLocalizer(options)

    return rememberTickingValue(localizer, instant, *keys) {
        val relative = instant - Clock.System.now()

        localize(localizer, relative)
    }.string()
}
