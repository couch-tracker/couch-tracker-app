package io.github.couchtracker.intl.datetime

import android.content.Context
import android.icu.util.ULocale
import dev.mmauro.datetimepolyglot.TickingValue
import dev.mmauro.datetimepolyglot.Zoned
import dev.mmauro.datetimepolyglot.localizers.absolute.DateStyle
import dev.mmauro.datetimepolyglot.localizers.absolute.DurationOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalDateTimeOptions
import dev.mmauro.datetimepolyglot.localizers.absolute.LocalTimeStyle
import dev.mmauro.datetimepolyglot.localizers.dynamic.DynamicDateAbsoluteTimeOptions
import dev.mmauro.datetimepolyglot.localizers.relative.RelativeDateAbsoluteTimeOptions
import dev.mmauro.datetimepolyglot.localizers.relative.RelativeLocalDateOptions
import dev.mmauro.datetimepolyglot.styles.DurationStyle
import dev.mmauro.datetimepolyglot.styles.RelativeUnitStyle
import io.github.couchtracker.R
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.runner.junit4.KotestTestRunner
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Current date used in tests. This is a Thursday
 */
private val NOW_DATE = LocalDateTime.parse("2026-01-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

@RunWith(KotestTestRunner::class)
class DynamicDateAbsoluteTimeWithDurationLocalizerTest : FunSpec(
    {
        // Disabled for now, needs to be moved to instrumented tests as we are using Android's ULocale now
        xcontext("localize") {
            context("dates within duration threshold") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(mockContext(ULocale.ENGLISH), locale = ULocale.ENGLISH),
                        LocalDateTime.parse("2026-01-01T23:00:00"),
                        TickingValue("today, 11:00 PM (in 23h)", nextTick = 1.nanoseconds),
                    ),
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(mockContext(ULocale.ENGLISH), locale = ULocale.ENGLISH),
                        NOW_DATE,
                        TickingValue("today, 12:00 AM (in 0m)", nextTick = 1.nanoseconds),
                    ),
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(mockContext(ULocale.ENGLISH), locale = ULocale.ENGLISH),
                        LocalDateTime.parse("2026-01-01T00:00:05"),
                        TickingValue("today, 12:00 AM (in 0m)", nextTick = 5.seconds + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(
                            context = mockContext(ULocale.ENGLISH),
                            locale = ULocale.ENGLISH,
                            options = DynamicDateAbsoluteTimeWithDurationOptions(
                                dateTimeOptions = DynamicDateAbsoluteTimeOptions(
                                    relativeOptions = RelativeDateAbsoluteTimeOptions(
                                        dateOptions = RelativeLocalDateOptions(
                                            style = RelativeUnitStyle.SHORT,
                                        ),
                                    ),
                                    absoluteOptions = LocalDateTimeOptions(DateStyle.MEDIUM, LocalTimeStyle.SHORT),
                                ),
                                durationOptions = DurationOptions(style = DurationStyle.WIDE),
                            ),
                        ),
                        LocalDateTime.parse("2026-01-01T15:05:00"),
                        TickingValue("today, 3:05 PM (in 15 hours, 5 minutes)", nextTick = 1.nanoseconds),
                    ),
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(
                            context = mockContext(ULocale.ITALIAN),
                            locale = ULocale.ITALIAN,
                            options = DynamicDateAbsoluteTimeWithDurationOptions(
                                dateTimeOptions = DynamicDateAbsoluteTimeWithDurationOptions.DEFAULT_DATE_TIME_OPTIONS.copy(
                                    absoluteOptions = LocalDateTimeOptions(DateStyle.MEDIUM, LocalTimeStyle.MEDIUM),
                                ),
                                durationOptions = DurationOptions(
                                    minUnit = DurationUnit.SECONDS,
                                    maxUnits = 3,
                                ),
                            ),
                        ),
                        LocalDateTime.parse("2025-12-31T19:56:54"),
                        TickingValue("ieri, 19:56:54 (4h 3min 6s fa)", nextTick = 1.seconds),
                    ),
                    tuple(
                        DynamicDateAbsoluteTimeWithDurationLocalizer(mockContext(ULocale.ENGLISH), locale = ULocale.ENGLISH),
                        LocalDateTime.parse("2025-12-31T23:59:59.999999999"),
                        TickingValue("yesterday, 11:59 PM (0m ago)", nextTick = 1.minutes - 1.nanoseconds),
                    ),
                ) { (localizer, localDateTime, expected) ->
                    localizer.localize(localDateTime, NOW) shouldBe expected
                }
            }
        }
    },
)

private fun mockContext(locale: ULocale) = mockk<Context> {
    every { getString(R.string.duration_x_ago, *anyVararg<Any>()) } answers {
        val param = secondArg<Array<Any>>().single() as String
        when (locale) {
            ULocale.ENGLISH -> "$param ago"
            ULocale.ITALIAN -> "$param fa"
            else -> throw UnsupportedOperationException("Unsupported test locale $locale")
        }
    }
    every { getString(R.string.duration_in_x, *anyVararg<Any>()) } answers {
        val param = secondArg<Array<Any>>().single() as String
        when (locale) {
            ULocale.ENGLISH -> "in $param"
            ULocale.ITALIAN -> "tra $param"
            else -> throw UnsupportedOperationException("Unsupported test locale $locale")
        }
    }
    every { getString(R.string.parenthesize, *anyVararg<Any>()) } answers {
        val (p1, p2) = secondArg<Array<Any>>()
        "$p1 ($p2)"
    }
}
