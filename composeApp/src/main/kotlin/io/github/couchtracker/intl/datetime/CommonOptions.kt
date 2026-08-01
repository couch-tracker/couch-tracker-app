package io.github.couchtracker.intl.datetime

import dev.mmauro.datetimepolyglot.localizers.absolute.DateComponents
import dev.mmauro.datetimepolyglot.localizers.absolute.DateStyle
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateTimeOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.TimeStyle
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicLocalDateOptions
import dev.mmauro.datetimepolyglot.styles.DayOfMonthStyle
import dev.mmauro.datetimepolyglot.styles.DayOfWeekStyle
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import dev.mmauro.datetimepolyglot.styles.MonthStyle

val RUNTIME_LOCALIZER_OPTIONS = DurationOptions(
    style = DurationStyle.SHORT,
)

val PDT_FULL_LOCALIZER_OPTIONS = PartialDateTimeOptions(
    monthStyle = MonthStyle.WIDE,
    dayOfWeekStyle = DayOfWeekStyle.WIDE,
)

val LOCAL_DATE_TIME_FULL_LOCALIZER_OPTIONS = LocalDateTimeOptions(
    dateOptions = DateStyle.FULL,
    timeOptions = TimeStyle.Local.MEDIUM,
)

val EPISODE_FIRST_AIRDATE_LOCALIZER_OPTIONS = DynamicLocalDateOptions(
    absoluteOptions = DateComponents(
        monthStyle = MonthStyle.ABBREVIATED,
        dayOfMonthStyle = DayOfMonthStyle.NUMERIC,
        dayOfWeekStyle = DayOfWeekStyle.ABBREVIATED,
    ),
)
