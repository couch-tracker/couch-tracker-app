package io.github.couchtracker.intl.datetime

import android.content.Context
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.plus
import io.github.couchtracker.utils.toLocalDateTime
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Current date used in tests. This is a Thursday
 */
private val NOW_DATE = LocalDateTime.parse("2026-01-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

class DynamicLocalDateTimeFormatterTest : FunSpec(
    {
        context("format") {
            context("dates over relative threshold are completely absolute") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                        MaybeZoned(
                            value = LocalDateTime.parse("2026-01-25T15:50:00"),
                            timeZone = null,
                        ),
                        TickingValue("Jan 25, 2026, 3:50 PM", nextTick = 14.days + 15.hours + 50.minutes + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ENGLISH),
                            locale = ULocale.ENGLISH,
                            absoluteDateSkeletons = Skeletons.NUMERIC_DATE,
                        ),
                        MaybeZoned(
                            value = LocalDateTime.parse("2026-01-25T00:00:00"),
                            timeZone = NOW.timeZone,
                        ),
                        TickingValue("01/25/2026, 12:00 AM", nextTick = 14.days + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ITALIAN),
                            locale = ULocale.ITALIAN,
                            timeSkeleton = TimeSkeleton.SECONDS,
                        ),
                        MaybeZoned(
                            value = LocalDateTime.parse("2020-01-31T15:16:17"),
                            timeZone = TimeZone.of("Europe/Rome"),
                        ),
                        TickingValue("31 gen 2020, 15:16:17 Europe/Rome", nextTick = null),
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale("es")),
                            locale = ULocale("es"),
                            timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION,
                        ),
                        MaybeZoned(
                            value = LocalDateTime.parse("1980-07-15T17:25:00"),
                            timeZone = TimeZone.of("Europe/Madrid"),
                        ),
                        TickingValue("15 jul 1980, 17:25 hora de verano de Europa central", nextTick = null),
                    ),
                ) { (formatter, dateTime, expected) ->
                    formatter.format(dateTime, NOW) shouldBe expected
                }
            }
            context("dates within relative threshold") {
                context("dates over duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-06T04:03:02"),
                                timeZone = null,
                            ),
                            TickingValue("next Tuesday, 4:03 AM", nextTick = 3.days),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-09T15:00:00"),
                                timeZone = null,
                            ),
                            TickingValue("in 8 days, 3:00 PM", nextTick = 1.days),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2025-12-28T19:56:54"),
                                timeZone = NOW.timeZone,
                            ),
                            TickingValue("4 giorni fa, 19:56:54", nextTick = 1.days),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-10T01:00:00"),
                                timeZone = TimeZone.of("Europe/Rome"),
                            ),
                            TickingValue("tra 9 giorni, 01:00 +01:00", nextTick = 1.days),
                        ),
                    ) { (formatter, localDateTime, expected) ->
                        formatter.format(localDateTime, NOW) shouldBe expected
                    }
                }
                context("dates within duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T23:00:00"),
                                timeZone = null,
                            ),
                            TickingValue("today, 11:00 PM (in 23h)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                                relativeDurationFormatWidth = MeasureFormat.FormatWidth.WIDE,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T15:05:00"),
                                timeZone = null,
                            ),
                            TickingValue("today, 3:05 PM (in 15 hours, 5 minutes)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                                relativeDurationMaxUnits = 3,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2025-12-31T19:56:54"),
                                timeZone = null,
                            ),
                            TickingValue("ieri, 19:56:54 (4h 3min 6s fa)", nextTick = 1.seconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            MaybeZoned(NOW_DATE, NOW.timeZone),
                            TickingValue("today, 12:00 AM (in 0m)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2025-12-31T23:59:59.999999999"),
                                timeZone = null,
                            ),
                            TickingValue("yesterday, 11:59 PM (0m ago)", nextTick = 1.minutes - 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ITALIAN), ULocale.ITALIAN),
                            MaybeZoned(NOW_DATE, TimeZone.of("Europe/Rome")),
                            TickingValue("oggi, 00:00 Europe/Rome (1h fa)", nextTick = 1.minutes),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T10:00:00"),
                                timeZone = TimeZone.of("Australia/Sydney"),
                            ),
                            TickingValue("today, 10:00 AM Australian Eastern Daylight Time (1h ago)", nextTick = 1.minutes),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                            ),
                            MaybeZoned(
                                value = LocalDateTime.parse("2025-12-31T21:00:00"),
                                timeZone = TimeZone.of("America/Los_Angeles"),
                            ),
                            TickingValue("yesterday, 9:00 PM -08:00 (in 5h)", nextTick = 1.nanoseconds),
                        ),
                    ) { (formatter, localDateTime, expected) ->
                        formatter.format(localDateTime, NOW) shouldBe expected
                    }
                }
            }

            context("nextTick") {
                // Kotlin Durations have a max precision of +/- 146 years when using nanosecond precision
                // For this reason the range of dates is limited to a 140 years period so that the next tick can never be bigger than this
                // limit and cause a test failure due to non-perfect precision
                val dateRange = LocalDateTime.parse("1950-01-01T00:00")..LocalDateTime.parse("2090-01-01T00:00:00")
                val formatter = DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH)
                nextTickPredictsChangeTestMaybeZoned(
                    arb = Arb.maybeZoned(Arb.kotlinLocalDateTime()).filter { it.value in dateRange },
                    valueFromInstant = { it.value.toLocalDateTime(it.timeZone) },
                    format = { dateTime, now -> formatter.format(dateTime, now) },
                    nowRange = dateRange.start.toInstant(TimeZone.UTC)..dateRange.endInclusive.toInstant(TimeZone.UTC),
                )
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

private fun DynamicLocalDateTimeFormatter.format(diff: Duration): TickingValue<String> {
    return format(MaybeZoned((NOW + diff).toLocalDateTime(), timeZone = null), NOW)
}
