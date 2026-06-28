@file:OptIn(ExperimentalZonedLocalizer::class)

package io.github.couchtracker.intl.datetime

import android.icu.util.ULocale
import androidx.compose.runtime.Composable
import dev.mmauro.datetimepolyglot.Zoned
import dev.mmauro.datetimepolyglot.localizers.ExperimentalZonedLocalizer
import dev.mmauro.datetimepolyglot.localizers.PolyglotDateTimeLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.DateComponents
import dev.mmauro.datetimepolyglot.localizers.absolute.DateStyleOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateTimeLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateTimeOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.TimeComponents
import dev.mmauro.datetimepolyglot.localizers.absolute.TimeOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.YearLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.YearMonthLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.YearMonthOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.YearOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedDateOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedInstantLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedInstantOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedLocalDateLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedYearLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedYearMonthLocalizer
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedYearMonthOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.ZonedYearOptions
import dev.mmauro.datetimepolyglot.localizers.standalone.TimeZoneOptions
import dev.mmauro.datetimepolyglot.styles.DayOfMonthStyle
import dev.mmauro.datetimepolyglot.styles.DayOfWeekStyle
import dev.mmauro.datetimepolyglot.styles.DayPeriodStyle
import dev.mmauro.datetimepolyglot.styles.EraStyle
import dev.mmauro.datetimepolyglot.styles.HourStyle
import dev.mmauro.datetimepolyglot.styles.MinuteStyle
import dev.mmauro.datetimepolyglot.styles.MonthStyle
import dev.mmauro.datetimepolyglot.styles.SecondStyle
import dev.mmauro.datetimepolyglot.styles.TimeZoneStyle
import dev.mmauro.datetimepolyglot.styles.YearStyle
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime

data class PartialDateTimeOptions(
    val yearOptions: YearOptions,
    val yearMonthOptions: YearMonthOptions,
    val dateOptions: DateStyleOptions,
    val dateTimeOptions: LocalDateTimeOptions,
    val zonedYearOptions: ZonedYearOptions,
    val zonedYearMonthOptions: ZonedYearMonthOptions,
    val zonedDateOptions: ZonedDateOptions,
    val zonedInstantOptions: ZonedInstantOptions,
) {

    constructor(
        yearOptions: YearOptions,
        yearMonthOptions: YearMonthOptions,
        dateOptions: DateStyleOptions,
        timeOptions: TimeOptions<TimeComponents.Local>,
        timeZoneOptions: TimeZoneOptions,
    ) : this(
        yearOptions = yearOptions,
        yearMonthOptions = yearMonthOptions,
        dateOptions = dateOptions,
        dateTimeOptions = LocalDateTimeOptions(dateOptions, timeOptions),
        zonedYearOptions = ZonedYearOptions(yearOptions, timeZoneOptions),
        zonedYearMonthOptions = ZonedYearMonthOptions(yearMonthOptions, timeZoneOptions),
        zonedDateOptions = ZonedDateOptions(dateOptions, timeZoneOptions),
        zonedInstantOptions = ZonedInstantOptions(
            dateOptions,
            TimeOptions(
                styleOptions = TimeComponents.Zoned(
                    hourStyle = timeOptions.styleOptions.hourStyle,
                    minuteStyle = timeOptions.styleOptions.minuteStyle,
                    secondStyle = timeOptions.styleOptions.secondStyle,
                    fractionalSecondDigits = timeOptions.styleOptions.fractionalSecondDigits,
                    dayPeriodStyle = timeOptions.styleOptions.dayPeriodStyle,
                    timeZoneStyle = timeZoneOptions.style,
                ),
                hourCycle = timeOptions.hourCycle,
            ),
        ),
    )

    constructor(
        dateOptions: DateComponents,
        timeOptions: TimeOptions<TimeComponents.Local>,
        timeZoneOptions: TimeZoneOptions,
    ) : this(
        yearOptions = YearOptions(eraStyle = dateOptions.eraStyle, yearStyle = dateOptions.yearStyle),
        yearMonthOptions = YearMonthOptions(
            eraStyle = dateOptions.eraStyle,
            yearStyle = dateOptions.yearStyle,
            monthStyle = dateOptions.monthStyle,
        ),
        dateOptions = dateOptions,
        timeOptions = timeOptions,
        timeZoneOptions = timeZoneOptions,
    )

    constructor(
        eraStyle: EraStyle? = null,
        yearStyle: YearStyle = YearStyle.NUMERIC_PADDED_4_DIGITS,
        monthStyle: MonthStyle,
        dayOfMonthStyle: DayOfMonthStyle = DayOfMonthStyle.NUMERIC,
        dayOfWeekStyle: DayOfWeekStyle? = null,
        hourStyle: HourStyle = HourStyle.NUMERIC,
        minuteStyle: MinuteStyle? = MinuteStyle.NUMERIC,
        secondStyle: SecondStyle? = null,
        fractionalSecondDigits: Int = 0,
        dayPeriodStyle: DayPeriodStyle? = null,
        timeZoneStyle: TimeZoneStyle.Generic = TimeZoneStyle.Generic.LOCATION,
    ) : this(
        dateOptions = DateComponents(eraStyle, yearStyle, monthStyle, dayOfMonthStyle, dayOfWeekStyle),
        timeOptions = TimeOptions(TimeComponents.Local(hourStyle, minuteStyle, secondStyle, fractionalSecondDigits, dayPeriodStyle)),
        timeZoneOptions = TimeZoneOptions(timeZoneStyle),
    )
}

class PartialDateTimeLocalizer(
    options: PartialDateTimeOptions,
    locale: ULocale = ULocale.getDefault(),
) : PolyglotDateTimeLocalizer<PartialDateTime> {

    private val yearLocalizer by lazy { YearLocalizer(options.yearOptions, locale) }
    private val yearMonthLocalizer by lazy { YearMonthLocalizer(options.yearMonthOptions, locale) }
    private val dateLocalizer by lazy { LocalDateLocalizer(options.dateOptions, locale) }
    private val dateTimeLocalizer by lazy { LocalDateTimeLocalizer(options.dateTimeOptions, locale) }

    private val zonedYearLocalizer by lazy { ZonedYearLocalizer(options.zonedYearOptions, locale) }
    private val zonedYearMonthLocalizer by lazy { ZonedYearMonthLocalizer(options.zonedYearMonthOptions, locale) }
    private val zonedDateLocalizer by lazy { ZonedLocalDateLocalizer(options.zonedDateOptions, locale) }
    private val zonedInstantLocalizer by lazy { ZonedInstantLocalizer(options.zonedInstantOptions, locale) }

    override fun localize(value: PartialDateTime): String {
        return when (value) {
            is PartialDateTime.Local.Year -> yearLocalizer.localize(value.year)
            is PartialDateTime.Local.YearMonth -> yearMonthLocalizer.localize(value.yearMonth)
            is PartialDateTime.Local.Date -> dateLocalizer.localize(value.date)
            is PartialDateTime.Local.DateTime -> dateTimeLocalizer.localize(value.dateTime)
            is PartialDateTime.Zoned -> when (val local = value.local) {
                is PartialDateTime.Local.Year -> zonedYearLocalizer.localize(Zoned(local.year, value.zone))
                is PartialDateTime.Local.YearMonth -> zonedYearMonthLocalizer.localize(Zoned(local.yearMonth, value.zone))
                is PartialDateTime.Local.Date -> zonedDateLocalizer.localize(Zoned(local.date, value.zone))
                is PartialDateTime.Local.DateTime -> zonedInstantLocalizer.localize(Zoned(value.toInstant(), value.zone))
            }
        }
    }
}

fun PartialDateTime.localize(
    options: PartialDateTimeOptions,
    locale: ULocale = ULocale.getDefault(),
): String {
    return PartialDateTimeLocalizer(options, locale).localize(this)
}

@Composable
fun rememberPartialDateTimeLocalizer(options: PartialDateTimeOptions): PartialDateTimeLocalizer {
    return rememberLocalizer(options, ::PartialDateTimeLocalizer)
}
