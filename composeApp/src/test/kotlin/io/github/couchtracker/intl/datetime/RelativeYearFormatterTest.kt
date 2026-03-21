package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.toLocalDateTime
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withContexts
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.year
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlin.time.Instant

private val NOW_DATE = LocalDate.parse("2026-04-01")
private val NOW = Zoned(NOW_DATE.atStartOfDayIn(TimeZone.UTC), TimeZone.UTC)

class RelativeYearFormatterTest : FunSpec(
    {
        context("format") {
            context("formatted string") {
                context("number of days") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -36, "36 years ago"),
                        tuple(ULocale.ENGLISH, -6, "6 years ago"),
                        tuple(ULocale.ENGLISH, -2, "2 years ago"),
                        tuple(ULocale.ENGLISH, 2, "in 2 years"),
                        tuple(ULocale.ENGLISH, 74, "in 74 years"),
                        tuple(ULocale.ITALIAN, -36, "36 anni fa"),
                        tuple(ULocale.ITALIAN, -6, "6 anni fa"),
                        tuple(ULocale.ITALIAN, 4, "tra 4 anni"),
                    ) { (locale, year, expected) ->
                        RelativeYearFormatter(locale).format(year).value shouldBe expected
                    }
                }

                context("single word") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -1, "last year"),
                        tuple(ULocale.ENGLISH, 0, "this year"),
                        tuple(ULocale.ENGLISH, 1, "next year"),
                        tuple(ULocale.ITALIAN, -1, "anno scorso"),
                        tuple(ULocale.ITALIAN, 0, "quest’anno"),
                        tuple(ULocale.ITALIAN, 1, "anno prossimo"),
                    ) { (locale, year, expected) ->
                        RelativeYearFormatter(locale).format(year).value shouldBe expected
                    }
                }

                context("number format follows locale convention") {
                    withTests(
                        nameFn = { it.first.toString() },
                        ULocale.ENGLISH to "in 1,000,000 years",
                        ULocale.ITALIAN to "tra 1.000.000 anni",
                        ULocale("hi") to "10,00,000 वर्ष में",
                    ) { (locale, expected) ->
                        RelativeYearFormatter(locale).format(1_000_000).value shouldBe expected
                    }
                }

                context("different styles are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(RelativeDateTimeFormatter.Style.LONG, -1, "last year"),
                        tuple(RelativeDateTimeFormatter.Style.LONG, 10, "in 10 years"),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, -1, "last yr."),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, 2, "in 2 yr."),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, -1, "last yr."),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, 10, "in 10y"),
                    ) { (style, year, expected) ->
                        val formatter = RelativeYearFormatter(locale = ULocale.ENGLISH, style = style)
                        formatter.format(year).value shouldBe expected
                    }
                }
                context("different capitalizations are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(DisplayContext.CAPITALIZATION_NONE, 0, "this year"),
                        tuple(DisplayContext.CAPITALIZATION_NONE, 10, "in 10 years"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 0, "This year"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 10, "In 10 years"),
                    ) { (capitalizationContext, year, expected) ->
                        val formatter = RelativeYearFormatter(locale = ULocale.ENGLISH, capitalizationContext = capitalizationContext)
                        formatter.format(year).value shouldBe expected
                    }
                }
            }

            context("next tick") {
                val formatter = RelativeYearFormatter(ULocale.ENGLISH)

                nextTickPredictsChangeTestWithNow(
                    arbitraryArb = Arb.year().map { it.value },
                    smallArb = { Arb.element(it.toLocalDateTime().year) },
                    format = formatter::format,
                )
            }
        }
    },
)

private fun RelativeYearFormatter.format(diff: Int) = formatAndTestNextTick(NOW_DATE.year + diff, NOW)
private fun RelativeYearFormatter.formatAndTestNextTick(year: Int, now: Zoned<Instant>) = formatAndTestNextTick(year, now, ::format)
