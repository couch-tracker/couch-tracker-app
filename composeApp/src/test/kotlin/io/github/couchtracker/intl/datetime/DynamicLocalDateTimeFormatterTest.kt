package io.github.couchtracker.intl.datetime

import android.content.Context
import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.R
import io.github.couchtracker.utils.TickingValue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.filter
import io.kotest.property.arbitrary.kotlinInstant
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.zoneId
import io.kotest.property.checkAll
import io.mockk.every
import io.mockk.mockk
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.min
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
                        "Jan 25, 2026, 3:50 PM",
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ENGLISH),
                            locale = ULocale.ENGLISH,
                            absoluteDateSkeletons = Skeletons.NUMERIC_DATE,
                        ),
                        LocalDateTime.parse("2026-01-25T00:00:00"),
                        "01/25/2026, 12:00 AM",
                    ),
                    tuple(
                        DynamicLocalDateTimeFormatter(
                            context = mockContext(ULocale.ITALIAN),
                            locale = ULocale.ITALIAN,
                            timeSkeleton = TimeSkeleton.SECONDS,
                        ),
                        LocalDateTime.parse("2020-01-31T15:16:17"),
                        "31 gen 2020, 15:16:17",
                    ),
                ) { (formatter, dateTime, expected) ->
                    formatter.format(dateTime, NOW, TZ) shouldBe TickingValue(expected, nextTick = null)
                }
            }
            context("dates within relative threshold") {
                context("dates over duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            5.days + 4.hours + 3.minutes + 2.seconds,
                            "next Tuesday, 4:03 AM",
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                            ),
                            8.days + 15.hours,
                            "in 8 days, 3:00 PM",
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                            ),
                            -(3.days + 4.hours + 3.minutes + 6.seconds),
                            "4 giorni fa, 19:56:54",
                        ),
                    ) { (formatter, diff, expected) ->
                        formatter.format(diff) shouldBe TickingValue(expected, nextTick = 1.days)
                    }
                }
                context("dates within duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH),
                            23.hours,
                            TickingValue("today, 11:00 PM (in 23h)", nextTick = null),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ENGLISH),
                                locale = ULocale.ENGLISH,
                                relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                                relativeDurationFormatWidth = MeasureFormat.FormatWidth.WIDE,
                            ),
                            15.hours + 5.minutes,
                            TickingValue("today, 3:05 PM (in 15 hours, 5 minutes)", nextTick = null),
                        ),
                        tuple(
                            DynamicLocalDateTimeFormatter(
                                context = mockContext(ULocale.ITALIAN),
                                locale = ULocale.ITALIAN,
                                timeSkeleton = TimeSkeleton.SECONDS,
                                relativeDurationMaxUnits = 3,
                            ),
                            -(4.hours + 3.minutes + 6.seconds),
                            TickingValue("ieri, 19:56:54 (4h 3min fa)", nextTick = null),
                        ),
                    ) { (formatter, diff, expected) ->
                        formatter.format(diff) shouldBe expected
                    }
                }
            }

            test("nextTick correctly predicts date change") {
                val formatter = DynamicLocalDateTimeFormatter(mockContext(ULocale.ENGLISH), ULocale.ENGLISH)
                checkAll(
                    iterations = 100_000,
                    // Kotlin Durations have a max precision of +/- 146 years when using nanosecond precision
                    // Let's limit the range of dates so that the next tick can never be so big
                    Arb.localDateTime(java.time.LocalDateTime.parse("1950-01-01T00:00"), java.time.LocalDateTime.parse("2090-01-01T00:00:00"))
                        .map { it.toKotlinLocalDateTime() },
                    Arb.kotlinInstant(Instant.parse("1950-01-01T00:00:00Z"), Instant.parse("2090-01-01T00:00:00Z")),
                    Arb.zoneId().map { it.toKotlinTimeZone() }.filter { it == TimeZone.UTC },
                ) { dateTime, now, tz ->
                    val formatted = formatter.format(dateTime, now, tz)

                    if (formatted.nextTick != null) {
                        withClue("nextTick (${formatted.nextTick}) would be @ ${now.plus(formatted.nextTick)}") {
                            withClue("format at 1 nanosecond before nextTick should yield same value") {
                                formatter.format(dateTime, now + formatted.nextTick - 1.nanoseconds, tz) should {
                                    it.value shouldBe formatted.value
                                    it.nextTick shouldBe 1.nanoseconds
                                }
                            }

                            withClue("format at nextTick should yield different value") {
                                formatter.format(dateTime, now + formatted.nextTick, tz) should {
                                    it.value shouldNotBe formatted.value
                                }
                            }
                        }
                    } else {
                        withClue("format in the very far future should yield same value") {
                            formatter.format(dateTime, now + (365 * 100).days, tz) shouldBe formatted
                        }
                    }
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

private fun DynamicLocalDateTimeFormatter.format(diff: Duration): TickingValue<String> {
    return format((NOW + diff).toLocalDateTime(TZ), NOW, TZ)
}
