package io.github.couchtracker.intl.datetime

import android.content.Context
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.utils.TickingValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.map
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Current date used in tests. This is a Thursday
 */
private val NOW = Instant.parse("2026-01-01T00:00:00Z")
private val TZ = TimeZone.UTC

class DynamicLocalDateTimeFormatterTest : FunSpec(
    {
        context("format") {
            context("dates over relative threshold are completely absolute") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                        LocalDateTime.parse("2026-01-25T15:50:00"),
                        TickingValue("Jan 25, 2026, 3:50 PM", nextTick = 14.days + 15.hours + 50.minutes + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ENGLISH),
                            locale = ULocale.ENGLISH,
                            absoluteDateSkeletons = Skeletons.NUMERIC_DATE,
                        ),
                        LocalDateTime.parse("2026-01-25T00:00:00"),
                        TickingValue("01/25/2026, 12:00 AM", nextTick = 14.days + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ITALIAN),
                            locale = ULocale.ITALIAN,
                            timeSkeleton = TimeSkeleton.SECONDS,
                        ),
                        LocalDateTime.parse("2020-01-31T15:16:17"),
                        TickingValue("31 gen 2020, 15:16:17", nextTick = null),
                    ),
                ) { (formatter, dateTime, expected) ->
                    formatter.format(dateTime, NOW, TZ) shouldBe expected
                }
            }
            context("dates within relative threshold") {
                context("dates over duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            5.days + 4.hours + 3.minutes + 2.seconds,
                            TickingValue("next Tuesday, 4:03 AM", nextTick = 3.days),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                            ),
                            8.days + 15.hours,
                            TickingValue("in 8 days, 3:00 PM", nextTick = 1.days),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                            ),
                            -(3.days + 4.hours + 3.minutes + 6.seconds),
                            TickingValue("4 giorni fa, 19:56:54", nextTick = 1.days),
                        ),
                    ) { (formatter, diff, expected) ->
                        formatter.format(diff) shouldBe expected
                    }
                }
                context("dates within duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            23.hours,
                            TickingValue("today, 11:00 PM (in 23h)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                                relativeDurationFormatWidth = MeasureFormat.FormatWidth.WIDE,
                            ),
                            15.hours + 5.minutes,
                            TickingValue("today, 3:05 PM (in 15 hours, 5 minutes)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                                relativeDurationMaxUnits = 3,
                            ),
                            -(4.hours + 3.minutes + 6.seconds),
                            TickingValue("ieri, 19:56:54 (4h 3min 6s fa)", nextTick = 1.seconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            0.nanoseconds,
                            TickingValue("today, 12:00 AM (in 0m)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            -1.nanoseconds,
                            TickingValue("yesterday, 11:59 PM (0m ago)", nextTick = 1.minutes - 1.nanoseconds),
                        ),
                    ) { (formatter, diff, expected) ->
                        formatter.format(diff) shouldBe expected
                    }
                }
            }

            context("nextTick") {
                // Kotlin Durations have a max precision of +/- 146 years when using nanosecond precision
                // For this reason the range of dates is limited to a 140 years period so that the next tick can never be bigger than this
                // limit and cause a test failure due to non-perfect precision
                val dateRange = LocalDateTime.parse("1950-01-01T00:00")..LocalDateTime.parse("2090-01-01T00:00:00")
                val formatter = DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH)
                nextTickPredictsChangeTest(
                    arb = Arb.localDateTime().map { it.toKotlinLocalDateTime() }.filter { it in dateRange },
                    valueFromInstant = { instant, tz -> instant.toLocalDateTime(tz) },
                    format = { dateTime, now, tz -> formatter.format(dateTime, now, tz) },
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
    return format((NOW + diff).toLocalDateTime(TZ), NOW, TZ)
}
