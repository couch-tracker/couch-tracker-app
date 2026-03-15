package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withContexts
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.localDate
import io.kotest.property.arbitrary.map
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toKotlinLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Current date used in most tests. This is a Wednesday
 */
private val CURRENT_DATE = LocalDate.parse("2025-12-31")

class RelativeLocalDateFormatterTest : FunSpec(
    {
        context("format") {
            context("formatted string") {
                context("number of days") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -124, "124 days ago"),
                        tuple(ULocale.ENGLISH, -10, "10 days ago"),
                        tuple(ULocale.ENGLISH, -2, "2 days ago"),
                        tuple(ULocale.ENGLISH, 15, "in 15 days"),
                        tuple(ULocale.ENGLISH, 555, "in 555 days"),
                        tuple(ULocale.ITALIAN, -124, "124 giorni fa"),
                        tuple(ULocale.ITALIAN, -10, "10 giorni fa"),
                        tuple(ULocale.ITALIAN, -3, "3 giorni fa"),
                        tuple(ULocale.ITALIAN, 15, "tra 15 giorni"),
                        tuple(ULocale.ITALIAN, 555, "tra 555 giorni"),
                    ) { (locale, days, expected) ->
                        RelativeLocalDateFormatter(locale).format(days).value shouldBe expected
                    }
                }

                context("single word") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, -1, "yesterday"),
                        tuple(ULocale.ENGLISH, 0, "today"),
                        tuple(ULocale.ENGLISH, 1, "tomorrow"),
                        tuple(ULocale.ITALIAN, -2, "l’altro ieri"),
                        tuple(ULocale.ITALIAN, 2, "dopodomani"),
                    ) { (locale, days, expected) ->
                        RelativeLocalDateFormatter(locale).format(days).value shouldBe expected
                    }
                }

                context("up to 7 days in the future uses this/next day of week") {
                    withTests(
                        nameFn = { it.toString() },
                        tuple(ULocale.ENGLISH, 2, "this Friday"),
                        tuple(ULocale.ENGLISH, 3, "this Saturday"),
                        // English's first day of week is Sunday
                        tuple(ULocale.ENGLISH, 4, "next Sunday"),
                        tuple(ULocale.ENGLISH, 7, "next Wednesday"),
                        tuple(ULocale.ENGLISH, 8, "in 8 days"),

                        tuple(ULocale.ITALIAN, 3, "questo sabato"),
                        tuple(ULocale.ITALIAN, 4, "questa domenica"),
                        // Italian's first day of week is Monday
                        tuple(ULocale.ITALIAN, 5, "lunedì prossimo"),
                        tuple(ULocale.ITALIAN, 7, "mercoledì prossimo"),
                    ) { (locale, days, expectedFormat) ->
                        RelativeLocalDateFormatter(locale).format(days).value shouldBe expectedFormat
                    }
                }

                context("number format follows locale convention") {
                    withTests(
                        nameFn = { it.first.toString() },
                        ULocale.ENGLISH to "1,234,567",
                        ULocale.ITALIAN to "1.234.567",
                        ULocale("hi") to "12,34,567",
                    ) { (locale, expected) ->
                        RelativeLocalDateFormatter(locale).format(-1_234_567).value shouldContain expected
                    }
                }

                context("different styles are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(RelativeDateTimeFormatter.Style.LONG, -1, "yesterday"),
                        tuple(RelativeDateTimeFormatter.Style.LONG, 2, "this Friday"),
                        tuple(RelativeDateTimeFormatter.Style.LONG, 10, "in 10 days"),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, -1, "yesterday"),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, 2, "this Fri."),
                        tuple(RelativeDateTimeFormatter.Style.SHORT, 10, "in 10 days"),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, -1, "yesterday"),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, 2, "this F"),
                        tuple(RelativeDateTimeFormatter.Style.NARROW, 10, "in 10d"),
                    ) { (style, days, expected) ->
                        val formatter = RelativeLocalDateFormatter(locale = ULocale.ENGLISH, style = style)
                        formatter.format(days).value shouldBe expected
                    }
                }
                context("different capitalizations are respected") {
                    withContexts(
                        nameFn = { it.toString() },
                        tuple(DisplayContext.CAPITALIZATION_NONE, 0, "today"),
                        tuple(DisplayContext.CAPITALIZATION_NONE, 2, "this Friday"),
                        tuple(DisplayContext.CAPITALIZATION_NONE, 10, "in 10 days"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 0, "Today"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 2, "This Friday"),
                        tuple(DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE, 10, "In 10 days"),
                    ) { (capitalizationContext, days, expected) ->
                        val formatter = RelativeLocalDateFormatter(locale = ULocale.ENGLISH, capitalizationContext = capitalizationContext)
                        formatter.format(days).value shouldBe expected
                    }
                }
            }

            context("next tick") {
                val formatter = RelativeLocalDateFormatter(ULocale.ENGLISH)

                context("basic tests") {
                    val date = LocalDate.parse("2026-01-01")
                    withTests(
                        nameFn = { "${it.a} ${it.b}" },
                        tuple(
                            Instant.parse("2026-01-01T21:00:00Z"),
                            TimeZone.UTC,
                            3.hours,
                        ),
                        tuple(
                            Instant.parse("2026-01-01T23:59:59Z"),
                            TimeZone.UTC,
                            1.seconds,
                        ),
                        tuple(
                            Instant.parse("2026-01-01T22:35:41Z"),
                            TimeZone.of("Europe/Berlin"),
                            24.minutes + 19.seconds,
                        ),
                        // This format returns "this Thursday", which is valid for multiple days until "tomorrow" triggers
                        tuple(
                            Instant.parse("2025-12-28T06:00:00Z"),
                            TimeZone.UTC,
                            2.days + 18.hours,
                        ),
                    ) { (now, tz, expected) ->
                        formatter.format(date, now, tz).nextTick shouldBe expected
                    }
                }

                context("handles DST changes") {
                    val tz = TimeZone.of("Europe/Rome")
                    test("jump forward") {
                        val date = LocalDate.parse("2025-03-30")
                        formatter.format(date = date, now = date.atStartOfDayIn(tz), tz = tz).nextTick shouldBe 23.hours
                    }
                    test("just backward") {
                        val date = LocalDate.parse("2025-10-26")
                        formatter.format(date = date, now = date.atStartOfDayIn(tz), tz = tz).nextTick shouldBe 25.hours
                    }
                }
                nextTickPredictsChangeTest(
                    arb = Arb.localDate().map { it.toKotlinLocalDate() },
                    valueFromInstant = { instant, tz -> instant.toLocalDateTime(tz).date },
                    format = { date, now, tz -> formatter.format(date, now, tz) },
                )
            }
        }
    },
)

private fun RelativeLocalDateFormatter.format(days: Int): TickingValue<String> {
    val tz = TimeZone.UTC
    val now = CURRENT_DATE.atStartOfDayIn(tz)
    return format(CURRENT_DATE.plus(days, DateTimeUnit.DAY), now, tz)
}
