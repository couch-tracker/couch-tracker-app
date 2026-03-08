package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.ULocale
import io.github.couchtracker.intl.UnitsFormatter
import io.github.couchtracker.utils.toIbmIcuTimeUnit
import io.github.couchtracker.utils.toULocale
import io.github.couchtracker.utils.unitPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit

data class DurationFormatter(
    private val omitZeros: Boolean,
    private val minUnit: DurationUnit,
    private val maxUnits: Int = 2,
    private val measureFormat: MeasureFormat,
) {

    private val unitsFormatter = UnitsFormatter.Companion<Duration, DurationUnit>(
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        measureFormat = measureFormat,
        unitPart = { unitPart(it) },
        imbIcuUnit = { toIbmIcuTimeUnit() },
    )

    fun units(duration: Duration) = unitsFormatter.units(duration)

    fun format(duration: Duration): String {
        require(duration == Duration.ZERO || duration.isPositive()) { "duration must be positive or zero" }
        require(duration.isFinite()) { "duration must be finite" }

        return unitsFormatter.format(duration)
    }
}

/**
 * Utility to format a [Duration].
 *
 * This is a suspend function, as it would be too slow to run on the main thread.
 */
suspend fun Duration.format(
    locale: ULocale? = null,
    omitZeros: Boolean = true,
    minUnit: DurationUnit = DurationUnit.MINUTES,
    maxUnits: Int = 2,
    formatWidth: MeasureFormat.FormatWidth = MeasureFormat.FormatWidth.SHORT,
): String = withContext(Dispatchers.Default) {
    val locale = locale ?: Locale.getDefault().toULocale()
    DurationFormatter(
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        measureFormat = MeasureFormat.getInstance(locale, formatWidth),
    ).format(this@format)
}
