package io.github.couchtracker.intl

import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.Measure
import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.ULocale

/**
 * Class that formats using [MeasureFormat] a set of units [U] (of type [Long]), extracted from type [T].
 */
@Suppress("LongParameterList")
class UnitsFormatter<T, U : Enum<U>> private constructor(
    private val measureFormat: MeasureFormat,
    private val omitZeros: Boolean,
    private val minUnit: U,
    private val maxUnits: Int,
    private val enumValues: List<U>,
    private val unitPart: T.(U) -> Long,
    private val ibmIcuUnit: U.() -> MeasureUnit,
) {

    init {
        require(maxUnits > 0) { "maxUnits must be greater than zero" }
    }

    fun units(value: T) = enumValues
        .reversed()
        .filter { it >= minUnit }
        .dropWhile { value.unitPart(it) == 0L }
        .take(maxUnits)
        .ifEmpty { listOf(minUnit) }

    fun format(value: T): String {
        val units = units(value)

        val measures = units.map { Measure(value.unitPart(it), it.ibmIcuUnit()) }
        val filteredMeasures = measures
            .filter { !omitZeros || it.number != 0L }
            .takeIf { it.isNotEmpty() }
            ?: listOf(measures.last())

        return measureFormat.format(filteredMeasures)
    }

    companion object {
        /**
         * @param omitZeros whether to omit units that are zeros. Leading zero units are always omitted. If all units are zero, [minUnit] is shown
         * @param minUnit the minimum unit to show. Anything smaller than that is truncated
         * @param maxUnits maximum number of units to show. If [omitZeros] is true, omitted zero units _are_ considered.
         * @param measureFormat a [MeasureFormat] instance to format the units with
         * @param unitPart extracts from [T] the value for the unit [U]
         * @param ibmIcuUnit maps a unit [U] to the corresponding [MeasureUnit]
         */
        @Suppress("LongParameterList")
        internal inline operator fun <T, reified U : Enum<U>> invoke(
            locale: ULocale,
            formatWidth: FormatWidth,
            omitZeros: Boolean,
            minUnit: U,
            maxUnits: Int,
            noinline unitPart: T.(U) -> Long,
            noinline ibmIcuUnit: U.() -> MeasureUnit,
        ) = UnitsFormatter(
            measureFormat = MeasureFormat.getInstance(locale, formatWidth),
            omitZeros = omitZeros,
            minUnit = minUnit,
            maxUnits = maxUnits,
            enumValues = enumValues<U>().toList(),
            unitPart = unitPart,
            ibmIcuUnit = ibmIcuUnit,
        )
    }
}
