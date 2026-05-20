package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.RelativeDateTimeFormatter
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
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.yearMonth
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaYearMonth
import kotlinx.datetime.toKotlinYearMonth
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val NOW_DATE = LocalDateTime.parse("2026-06-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

class DynamicYearMonthFormatterTest : FunSpec(
    {
        context("format") {
            context("dates over relative threshold are absolute") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicYearMonthFormatter(ULocale.ENGLISH),
                        MaybeZoned(value = YearMonth(2026, Month.AUGUST), timeZone = null),
                        TickingValue("August 2026", nextTick = 30.days),
                    ),
                    tuple(
                        DynamicYearMonthFormatter(
                            locale = ULocale.ENGLISH,
                            timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                        ),
                        MaybeZoned(value = YearMonth(2020, Month.JUNE), timeZone = null),
                        TickingValue("June 2020", nextTick = null),
                    ),
                    tuple(
                        DynamicYearMonthFormatter(
                            locale = ULocale.ITALIAN,
                            timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION,
                        ),
                        MaybeZoned(
                            value = YearMonth(1994, Month.DECEMBER),
                            timeZone = TimeZone.of("Europe/Rome"),
                        ),
                        TickingValue("dicembre 1994, Ora standard dell’Europa centrale", nextTick = null),
                    ),
                    tuple(
                        DynamicYearMonthFormatter(
                            locale = ULocale("es"),
                            yearSkeleton = YearSkeleton.SHORT,
                            monthSkeleton = MonthSkeleton.ABBREVIATED,
                        ),
                        MaybeZoned(
                            value = YearMonth(1969, Month.OCTOBER),
                            timeZone = TimeZone.of("Europe/Madrid"),
                        ),
                        TickingValue("oct 69, Europe/Madrid", nextTick = null),
                    ),
                    tuple(
                        DynamicYearMonthFormatter(
                            locale = ULocale("es"),
                            yearSkeleton = YearSkeleton.SHORT,
                            monthSkeleton = MonthSkeleton.NUMERIC,
                        ),
                        MaybeZoned(
                            value = YearMonth(1969, Month.OCTOBER),
                            timeZone = null,
                        ),
                        TickingValue("10/69", nextTick = null),
                    ),
                ) { (formatter, year, expected) ->
                    formatter.formatAndTestNextTick(year, NOW) shouldBe expected
                }
            }
        }

        context("dates within relative threshold") {
            withTests(
                nameFn = { Pair(it.b, it.c).toString() },
                tuple(
                    DynamicYearMonthFormatter(ULocale.ENGLISH),
                    MaybeZoned(
                        value = YearMonth(2026, Month.JUNE),
                        timeZone = null,
                    ),
                    TickingValue("this month", nextTick = 30.days),
                ),
                tuple(
                    DynamicYearMonthFormatter(
                        locale = ULocale.ENGLISH,
                        relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                    ),
                    MaybeZoned(
                        value = YearMonth(2026, Month.MAY),
                        timeZone = null,
                    ),
                    TickingValue("last mo.", nextTick = 30.days),
                ),
                tuple(
                    DynamicYearMonthFormatter(ULocale.ITALIAN),
                    MaybeZoned(value = YearMonth(2026, Month.JULY), timeZone = TimeZone.of("Australia/Sydney")),
                    TickingValue("mese prossimo, Australia/Sydney", nextTick = 30.days),
                ),
                tuple(
                    DynamicYearMonthFormatter(
                        locale = ULocale.ITALIAN,
                        timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                    ),
                    MaybeZoned(
                        value = YearMonth(2026, Month.JUNE),
                        timeZone = TimeZone.of("Europe/Rome"),
                    ),
                    TickingValue("questo mese, +02:00", nextTick = 30.days),
                ),
            ) { (formatter, localDateTime, expected) ->
                formatter.formatAndTestNextTick(localDateTime, NOW) shouldBe expected
            }
        }

        context("nextTick") {
            val dateRange = LocalDateTime.parse("1950-01-01T00:00")..LocalDateTime.parse("2090-01-01T00:00:00")
            val formatter = DynamicYearMonthFormatter(locale = ULocale.ITALIAN)
            nextTickPredictsChangeTestMaybeZoned(
                arbitraryArb = Arb.maybeZoned(
                    Arb.yearMonth(
                        minYearMonth = YearMonth(1950, Month.JANUARY).toJavaYearMonth(),
                        maxYearMonth = YearMonth(2090, Month.DECEMBER).toJavaYearMonth(),
                    ).map { it.toKotlinYearMonth() },
                ),
                smallArb = { Arb.element(it.toLocalDateTime().let { ldt -> YearMonth(ldt.year, ldt.month) }) },
                format = formatter::format,
                nowRange = dateRange.start.toInstant(TimeZone.UTC)..dateRange.endInclusive.toInstant(TimeZone.UTC),
            )
        }
    },
)

private fun DynamicYearMonthFormatter.formatAndTestNextTick(dateTime: MaybeZoned<YearMonth>, now: Zoned<Instant>) =
    formatAndTestNextTick(dateTime, now, ::format)
