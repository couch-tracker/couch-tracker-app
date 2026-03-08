package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat
import io.github.couchtracker.intl.UnitsFormatter
import io.github.couchtracker.utils.DateTimePeriodUnit
import io.github.couchtracker.utils.isAnyComponentNegative
import io.github.couchtracker.utils.unitPart
import kotlinx.datetime.DateTimePeriod

data class DateTimePeriodFormatter(
    private val omitZeros: Boolean,
    private val minUnit: DateTimePeriodUnit,
    private val maxUnits: Int = 2,
    private val measureFormat: MeasureFormat,
) {

    private val unitsFormatter = UnitsFormatter.Companion<DateTimePeriod, DateTimePeriodUnit>(
        omitZeros = omitZeros,
        minUnit = minUnit,
        maxUnits = maxUnits,
        measureFormat = measureFormat,
        unitPart = { unitPart(it) },
        imbIcuUnit = { imbIcuUnit },
    )

    fun units(period: DateTimePeriod) = unitsFormatter.units(period)

    fun format(period: DateTimePeriod): String {
        require(!period.isAnyComponentNegative()) { "period must be positive or zero" }

        return unitsFormatter.format(period)
    }
}
