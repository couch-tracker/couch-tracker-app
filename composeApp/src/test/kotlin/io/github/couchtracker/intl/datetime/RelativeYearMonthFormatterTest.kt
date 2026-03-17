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
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.yearMonth
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinYearMonth
import kotlinx.datetime.yearMonth

private val NOW_DATE = LocalDate.parse("2026-04-01")
private val NOW = Zoned(NOW_DATE.atStartOfDayIn(TimeZone.UTC), TimeZone.UTC)

class RelativeYearMonthFormatterTest : FunSpec(
    {
        context("format") {
            context("formatted string") {
                context("number of days") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -36, "36 months ago"),
                        tuple(ULocale.ENGLISH, -6, "6 months ago"),
                        tuple(ULocale.ENGLISH, -2, "2 months ago"),
                        tuple(ULocale.ENGLISH, 2, "in 2 months"),
                        tuple(ULocale.ENGLISH, 74, "in 74 months"),
                        tuple(ULocale.ITALIAN, -36, "36 mesi fa"),
                        tuple(ULocale.ITALIAN, -6, "6 mesi fa"),
                        tuple(ULocale.ITALIAN, 4, "tra 4 mesi"),
                    ) { (locale, month, expected) ->
                        RelativeYearMonthFormatter(locale).format(month).value shouldBe expected
                    }
                }

                context("single word") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -1, "last month"),
                        tuple(ULocale.ENGLISH, 0, "this month"),
                        tuple(ULocale.ENGLISH, 1, "next month"),
                        tuple(ULocale.ITALIAN, -1, "mese scorso"),
                        tuple(ULocale.ITALIAN, 0, "questo mese"),
                        tuple(ULocale.ITALIAN, 1, "mese prossimo"),
                    ) { (locale, month, expected) ->
                        RelativeYearMonthFormatter(locale).format(month).value shouldBe expected
                    }
                }

                context("number format follows locale convention") {
                    withTests(
                        nameFn = { it.first.toString() },
                        ULocale.ENGLISH to "in 1,000,000 months",
                        ULocale.ITALIAN to "tra 1.000.000 mesi",
                        ULocale("hi") to "10,00,000 माह में",
                    ) { (locale, expected) ->
                        RelativeYearMonthFormatter(locale).format(1_000_000).value shouldBe expected
                    }
                }

                context("different styles are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(RelativeDateTimeFormatter.Style.LONG, -1, "last month"),
                        tuple(RelativeDateTimeFormatter.Style.LONG, 10, "in 10 months"),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, -1, "last mo."),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, 2, "in 2 mo."),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, -1, "last mo."),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, 10, "in 10mo"),
                    ) { (style, month, expected) ->
                        val formatter = RelativeYearMonthFormatter(locale = ULocale.ENGLISH, style = style)
                        formatter.format(month).value shouldBe expected
                    }
                }
                context("different capitalizations are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(DisplayContext.CAPITALIZATION_NONE, 0, "this month"),
                        tuple(DisplayContext.CAPITALIZATION_NONE, 10, "in 10 months"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 0, "This month"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 10, "In 10 months"),
                    ) { (capitalizationContext, month, expected) ->
                        val formatter = RelativeYearMonthFormatter(locale = ULocale.ENGLISH, capitalizationContext = capitalizationContext)
                        formatter.format(month).value shouldBe expected
                    }
                }
            }

            context("next tick") {
                val formatter = RelativeYearMonthFormatter(ULocale.ENGLISH)

                nextTickPredictsChangeTest(
                    arb = Arb.yearMonth().map { it.toKotlinYearMonth() },
                    valueFromInstant = { it.toLocalDateTime().date.yearMonth },
                    format = { yearMonth, now -> formatter.format(yearMonth, now) },
                )
            }
        }
    },
)

fun RelativeYearMonthFormatter.format(diff: Int) = format(NOW_DATE.yearMonth.plus(diff, DateTimeUnit.MONTH), NOW)
