package io.github.couchtracker.intl.datetime

import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.year
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

private val NOW_DATE = LocalDateTime.parse("2026-04-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

class DynamicYearFormatterTest : FunSpec(
    {
        test("test") {
            val now = Zoned(value = Instant.parse("2090-01-01T00:00:01Z"), timeZone = TimeZone.UTC)
            val year = MaybeZoned(2090, TimeZone.UTC)

            val formatted = DynamicYearFormatter(ULocale.ENGLISH).format(year, now)
            formatted
        }
        context("format") {
            xcontext("dates over relative threshold are completely absolute") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicYearFormatter(ULocale.ENGLISH),
                        MaybeZoned(value = 2030, timeZone = null),
                        TickingValue("2030", nextTick = 1006.days + 1.nanoseconds),
                    ),
                    tuple(
                        DynamicYearFormatter(
                            locale = ULocale.ENGLISH,
                            absoluteSkeleton = YearSkeleton.SHORT,
                        ),
                        MaybeZoned(
                            value = 1994,
                            timeZone = NOW.timeZone,
                        ),
                        TickingValue("94", nextTick = null),
                    ),
                    tuple(
                        DynamicYearFormatter(locale = ULocale.ENGLISH),
                        MaybeZoned(
                            value = 2013,
                            timeZone = TimeZone.of("Europe/Rome"),
                        ),
                        TickingValue("2013, Europe/Rome", nextTick = null),
                    ),
                    tuple(
                        DynamicYearFormatter(
                            locale = ULocale.ITALIAN,
                            timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION,
                        ),
                        MaybeZoned(
                            value = 2020,
                            timeZone = TimeZone.of("Europe/Rome"),
                        ),
                        TickingValue("2020, Ora standard dell’Europa centrale", nextTick = null),
                    ),
                ) { (formatter, year, expected) ->
                    formatter.format(year, NOW) shouldBe expected
                }
            }
            /*context("dates within relative threshold") {
                context("dates over duration threshold") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            DynamicYearFormatter(ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-06T04:03:02"),
                                timeZone = null,
                            ),
                            TickingValue("next Tuesday, 4:03 AM", nextTick = 3.days),
                        ),
                        tuple(
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T23:00:00"),
                                timeZone = null,
                            ),
                            TickingValue("today, 11:00 PM (in 23h)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(ULocale.ENGLISH),
                            MaybeZoned(NOW_DATE, NOW.timeZone),
                            TickingValue("today, 12:00 AM (in 0m)", nextTick = 1.nanoseconds),
                        ),
                        tuple(
                            DynamicYearFormatter(ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2025-12-31T23:59:59.999999999"),
                                timeZone = null,
                            ),
                            TickingValue("yesterday, 11:59 PM (0m ago)", nextTick = 1.minutes - 1.nanoseconds),
                        ),
                        tuple(
                            DynamicYearFormatter(mockContext(ULocale.ITALIAN), ULocale.ITALIAN),
                            MaybeZoned(NOW_DATE, TimeZone.of("Europe/Rome")),
                            TickingValue("oggi, 00:00 Europe/Rome (1h fa)", nextTick = 1.minutes),
                        ),
                        tuple(
                            DynamicYearFormatter(
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
                            DynamicYearFormatter(
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
            }*/

            context("nextTick") {
                val formatter = DynamicYearFormatter(ULocale.ENGLISH)
                val dateRange = LocalDateTime.parse("1950-01-01T00:00")..LocalDateTime.parse("2090-01-01T00:00:00")
                nextTickPredictsChangeTestMaybeZoned(
                    arb = Arb.maybeZoned(Arb.year().map { it.value }),
                    valueFromInstant = { it.toLocalDateTime().year },
                    format = { year, now -> formatter.format(year, now) },
                    nowRange = dateRange.start.toInstant(TimeZone.UTC)..dateRange.endInclusive.toInstant(TimeZone.UTC),
                )
            }
        }
    },
)
