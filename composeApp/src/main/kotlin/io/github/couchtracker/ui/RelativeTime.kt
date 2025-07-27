package io.github.couchtracker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.Measure
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.remainderUntilNextUnitBoundary
import io.github.couchtracker.utils.rememberTickingValue
import io.github.couchtracker.utils.toImbIcuTimeUnit
import io.github.couchtracker.utils.toULocale
import io.github.couchtracker.utils.unitPart
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.DurationUnit


@Composable
fun rememberRelativeDurationText(
    instant: Instant,
    omitZeros: Boolean = true,
    minUnit: DurationUnit = DurationUnit.MINUTES,
    maxUnits: Int = 2,
    formatWidth: MeasureFormat.FormatWidth = MeasureFormat.FormatWidth.SHORT,
    formatMapper: @Composable (String) -> String = { it },
    formatOverride: (Duration) -> TickingValue<Text>? = { null },
    locale: ULocale? = null,
): State<Text> {
    val locale = locale ?: LocalConfiguration.currentFirstLocale.toULocale()
    val formatter = remember(locale, formatWidth) { MeasureFormat.getInstance(locale, formatWidth) }

    return rememberTickingValue(instant) {
        val relative = instant - Clock.System.now()

        // Return overridden format if it's not null
        formatOverride(relative)?.let { return@rememberTickingValue it }

        val units = DurationUnit.entries
            .reversed()
            .filter { it >= minUnit }
            .dropWhile { relative.unitPart(it) == 0L }
            .take(maxUnits)
            .ifEmpty { listOf(minUnit) }

        val abs = relative.absoluteValue
        val measures = units.map { Measure(abs.unitPart(it), it.toImbIcuTimeUnit()) }
        val filteredMeasures = measures
            .filter { !omitZeros || it.number != 0L }
            .takeIf { it.isNotEmpty() }
            ?: listOf(measures.last())

        TickingValue(
            value = Text.Lambda { formatMapper(formatter.format(filteredMeasures)) },
            nextTick = units.minOf { relative.remainderUntilNextUnitBoundary(it) }.also {
                println("relative=$relative, nextTick=$it")
            },
        )
    }
}
