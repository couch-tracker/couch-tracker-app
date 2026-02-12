package io.github.couchtracker.ui

import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.toULocale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.DurationUnit

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
    RelativeDurationFormatter(
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        measureFormat = MeasureFormat.getInstance(locale, formatWidth),
    ).format(this@format).value
}
