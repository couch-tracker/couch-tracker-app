package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.LocaleData
import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.Zoned
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * Current date used in most tests. This is a Thursday
 */
private val NOW_DATE = LocalDateTime.parse("2026-01-01T00:00:00")
private val NOW = Zoned(NOW_DATE.toInstant(TimeZone.UTC), TimeZone.UTC)

class RelativeDateAbsoluteTimeFormatterTest : FunSpec(
    {
        context("format") {
            context("formatted value") {
                context("with null or same timezone as now") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            LocalDateTime.parse("2026-01-16T03:00:00"),
                            "in 15 days at 3:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            LocalDateTime.parse("2025-12-31T21:00:00"),
                            "yesterday at 9:00 PM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            LocalDateTime.parse("2025-12-11T19:00:00"),
                            "21 days ago at 7:00 PM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, timeSkeleton = TimeSkeleton.SECONDS),
                            LocalDateTime.parse("2026-01-01T04:13:05"),
                            "today at 4:13:05 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            LocalDateTime.parse("2026-01-04T04:00:00"),
                            "next Sunday at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, relativeDateStyle = RelativeDateTimeFormatter.Style.SHORT),
                            LocalDateTime.parse("2026-01-04T04:00:00"),
                            "next Sun. at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, dateFormatStyle = DateFormat.SHORT),
                            LocalDateTime.parse("2026-01-04T04:00:00"),
                            "next Sunday, 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, dateFormatStyle = DateFormat.LONG),
                            LocalDateTime.parse("2026-01-04T04:00:00"),
                            "next Sunday at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ITALIAN),
                            LocalDateTime.parse("2025-12-31T21:00:00"),
                            "ieri alle ore 21:00",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ITALIAN, dateFormatStyle = DateFormat.LONG),
                            LocalDateTime.parse("2026-01-06T15:00:00"),
                            "martedì prossimo alle ore 15:00",
                        ),
                    ) { (formatter, localDateTime, expected) ->
                        withClue("null timezone") {
                            formatter.format(MaybeZoned(localDateTime, timeZone = null), NOW).value shouldBe expected
                        }
                        withClue("with same timezone as now") {
                            formatter.format(MaybeZoned(localDateTime, timeZone = NOW.timeZone), NOW).value shouldBe expected
                        }
                    }
                }

                context("with different timezone from now") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T01:00:00"),
                                timeZone = TimeZone.of("Europe/Rome"),
                            ),
                            "today at 1:00 AM Europe/Rome",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, timeZoneSkeleton = TimezoneSkeleton.SPECIFIC_NON_LOCATION),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-02T00:00:00"),
                                timeZone = TimeZone.of("Europe/Berlin"),
                            ),
                            "tomorrow at 12:00 AM Central European Standard Time",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, timeZoneSkeleton = TimezoneSkeleton.OFFSET),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-01T06:00:00"),
                                timeZone = TimeZone.of("Australia/Sydney"),
                            ),
                            "today at 6:00 AM +11:00",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, timeZoneSkeleton = TimezoneSkeleton.OFFSET),
                            MaybeZoned(
                                value = LocalDateTime.parse("2026-01-20T19:00:00"),
                                timeZone = TimeZone.of("America/New_York"),
                            ),
                            "in 19 days at 7:00 PM -05:00",
                        ),
                    ) { (formatter, localDateTime, expected) ->
                        formatter.format(localDateTime, NOW).value shouldBe expected
                    }
                }

                test("must contain relative formatted local date") {
                    checkAll(
                        LocaleData().allLocales.exhaustive(),
                        Arb.maybeZoned(Arb.kotlinLocalDateTime()),
                        Arb.zonedInstant(),
                        Arb.enum<RelativeDateTimeFormatter.Style>(),
                    ) { locale, dateTime, now, relativeStyle ->
                        val dateString = RelativeLocalDateFormatter(locale, style = relativeStyle).format(dateTime.value.date, now).value
                        val formatted =
                            RelativeDateAbsoluteTimeFormatter(locale, relativeDateStyle = relativeStyle).format(dateTime, now).value
                        formatted shouldContain dateString
                    }
                }
            }

            context("nextTick") {
                val formatter = RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH)
                test("is always identical to RelativeLocalDateFormatter (as time is always absolute)") {
                    val dateFormatter = RelativeLocalDateFormatter(ULocale.ENGLISH)
                    checkAll(
                        Arb.maybeZoned(Arb.kotlinLocalDateTime()),
                        Arb.zonedInstant(),
                    ) { localDateTime, now ->
                        formatter.format(localDateTime, now).nextTick shouldBe dateFormatter.format(localDateTime.value.date, now).nextTick
                    }
                }

                nextTickPredictsChangeTest(
                    arb = Arb.maybeZoned(Arb.kotlinLocalDateTime()),
                    valueFromInstant = { zonedInstant ->
                        Arb.maybeZoned(zonedInstant.value)
                            .map { MaybeZoned(it.value.toLocalDateTime(it.timeZone ?: zonedInstant.timeZone), it.timeZone) }
                            .next()
                    },
                    format = { dateTime, now -> formatter.format(dateTime, now) },
                )
            }
        }
    },
)
