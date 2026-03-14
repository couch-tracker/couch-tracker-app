package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.ULocale
import io.github.couchtracker.intl.UnitsFormatter
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_FORMAT_WIDTH
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_MAX_UNITS
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_MIN_UNIT
import io.github.couchtracker.intl.datetime.DurationFormatter.Companion.DEFAULT_OMIT_ZEROS
import io.github.couchtracker.utils.toIbmIcuTimeUnit
import io.github.couchtracker.utils.toULocale
import io.github.couchtracker.utils.unitPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit

class DurationFormatter(
    locale: ULocale,
    formatWidth: FormatWidth = DEFAULT_FORMAT_WIDTH,
    private val omitZeros: Boolean = DEFAULT_OMIT_ZEROS,
    private val minUnit: DurationUnit = DEFAULT_MIN_UNIT,
    private val maxUnits: Int = DEFAULT_MAX_UNITS,
) {

    private val unitsFormatter = UnitsFormatter<Duration, DurationUnit>(
        locale = locale,
        formatWidth = formatWidth,
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        unitPart = { unitPart(it) },
        ibmIcuUnit = { toIbmIcuTimeUnit() },
    )

    fun units(duration: Duration) = unitsFormatter.units(duration)

    fun format(duration: Duration): String {
        require(duration == Duration.ZERO || duration.isPositive()) { "duration must be positive or zero" }
        require(duration.isFinite()) { "duration must be finite" }

        return unitsFormatter.format(duration)
    }

    companion object {
        const val DEFAULT_OMIT_ZEROS = true
        val DEFAULT_MIN_UNIT = DurationUnit.MINUTES
        const val DEFAULT_MAX_UNITS = 2
        val DEFAULT_FORMAT_WIDTH = FormatWidth.SHORT
    }
}

/**
 * Utility to format a [Duration].
 *
 * This is a suspend function, as it would be too slow to run on the main thread.
 */
suspend fun Duration.format(
    locale: ULocale? = null,
    formatWidth: FormatWidth = DEFAULT_FORMAT_WIDTH,
    omitZeros: Boolean = DEFAULT_OMIT_ZEROS,
    minUnit: DurationUnit = DEFAULT_MIN_UNIT,
    maxUnits: Int = DEFAULT_MAX_UNITS,
): String = withContext(Dispatchers.Default) {
    val locale = locale ?: Locale.getDefault().toULocale()
    DurationFormatter(
        locale = locale,
        formatWidth = formatWidth,
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
    ).format(this@format)
}
