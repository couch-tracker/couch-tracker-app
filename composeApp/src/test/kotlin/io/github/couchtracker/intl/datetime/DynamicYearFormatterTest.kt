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
import io.kotest.property.arbitrary.int
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

private val NOW_DATE = LocalDateTime.parse("2026-06-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

class DynamicYearFormatterTest : FunSpec(
    {
        context("format") {
            context("dates over relative threshold are completely absolute") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        DynamicYearFormatter(ULocale.ENGLISH),
                        MaybeZoned(value = 2028, timeZone = null),
                        TickingValue("2028", nextTick = 214.days),
                    ),
                    tuple(
                        DynamicYearFormatter(
                            locale = ULocale.ENGLISH,
                            timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                        ),
                        MaybeZoned(value = 2020, timeZone = null),
                        TickingValue("2020", nextTick = null),
                    ),
                    tuple(
                        DynamicYearFormatter(
                            locale = ULocale.ITALIAN,
                            timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION,
                        ),
                        MaybeZoned(
                            value = 1999,
                            timeZone = TimeZone.of("Europe/Rome"),
                        ),
                        TickingValue("1999, Ora standard dell’Europa centrale", nextTick = null),
                    ),
                    tuple(
                        DynamicYearFormatter(
                            locale = ULocale("es"),
                            absoluteSkeleton = YearSkeleton.SHORT,
                        ),
                        MaybeZoned(
                            value = 1969,
                            timeZone = TimeZone.of("Europe/Madrid"),
                        ),
                        TickingValue("69, Europe/Madrid", nextTick = null),
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
                    DynamicYearFormatter(ULocale.ENGLISH),
                    MaybeZoned(
                        value = 2026,
                        timeZone = null,
                    ),
                    TickingValue("this year", nextTick = 214.days),
                ),
                tuple(
                    DynamicYearFormatter(
                        locale = ULocale.ENGLISH,
                        relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT,
                    ),
                    MaybeZoned(
                        value = 2025,
                        timeZone = null,
                    ),
                    TickingValue("last yr.", nextTick = 214.days),
                ),
                tuple(
                    DynamicYearFormatter(ULocale.ITALIAN),
                    MaybeZoned(value = 2027, timeZone = TimeZone.of("America/New_York")),
                    TickingValue("anno prossimo, America/New_York", nextTick = 214.days),
                ),
                tuple(
                    DynamicYearFormatter(
                        locale = ULocale.ITALIAN,
                        timeZoneSkeleton = TimezoneSkeleton.OFFSET,
                    ),
                    MaybeZoned(
                        value = 2026,
                        timeZone = TimeZone.of("Europe/Rome"),
                    ),
                    TickingValue("quest’anno, +01:00", nextTick = 214.days),
                ),
            ) { (formatter, localDateTime, expected) ->
                formatter.formatAndTestNextTick(localDateTime, NOW) shouldBe expected
            }
        }

        context("nextTick") {
            val dateRange = LocalDateTime.parse("1950-01-01T00:00")..LocalDateTime.parse("2090-01-01T00:00:00")
            val formatter = DynamicYearFormatter(locale = ULocale.ITALIAN)
            nextTickPredictsChangeTestMaybeZoned(
                arbitraryArb = Arb.maybeZoned(Arb.int(min = 1950, max = 2090)),
                smallArb = { Arb.element(it.toLocalDateTime().year) },
                format = formatter::format,
                nowRange = dateRange.start.toInstant(TimeZone.UTC)..dateRange.endInclusive.toInstant(TimeZone.UTC),
            )
        }
    },
)

private fun DynamicYearFormatter.formatAndTestNextTick(dateTime: MaybeZoned<Int>, now: Zoned<Instant>) =
    formatAndTestNextTick(dateTime, now, ::format)
