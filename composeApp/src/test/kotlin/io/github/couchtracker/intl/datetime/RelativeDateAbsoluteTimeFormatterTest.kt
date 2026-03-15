package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.LocaleData
import io.github.couchtracker.utils.TickingValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.kotlinInstant
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.zoneId
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

/**
 * Current date used in most tests. This is a Thursday
 */
private val NOW = Instant.parse("2026-01-01T00:00:00Z")
private val TZ = TimeZone.UTC

class RelativeDateAbsoluteTimeFormatterTest : FunSpec(
    {
        context("format") {
            context("formatted value") {
                context("works") {
                    withTests(
                        nameFn = { Pair(it.b, it.c).toString() },
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            15.days + 3.hours,
                            "in 15 days at 3:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            -3.hours,
                            "yesterday at 9:00 PM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            -(20.days + 5.hours),
                            "21 days ago at 7:00 PM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, timeSkeleton = TimeSkeleton.SECONDS),
                            4.hours + 13.minutes + 5.seconds,
                            "today at 4:13:05 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH),
                            3.days + 4.hours,
                            "next Sunday at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, relativeStyle = RelativeDateTimeFormatter.Style.SHORT),
                            3.days + 4.hours,
                            "next Sun. at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, dateFormatStyle = DateFormat.SHORT),
                            3.days + 4.hours,
                            "next Sunday, 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH, dateFormatStyle = DateFormat.LONG),
                            3.days + 4.hours,
                            "next Sunday at 4:00 AM",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ITALIAN),
                            -3.hours,
                            "ieri alle ore 21:00",
                        ),
                        tuple(
                            RelativeDateAbsoluteTimeFormatter(ULocale.ITALIAN, dateFormatStyle = DateFormat.LONG),
                            5.days + 15.hours,
                            "martedì prossimo alle ore 15:00",
                        ),
                    ) { (formatter, diff, expected) ->
                        formatter.format(diff).value shouldBe expected
                    }
                }

                test("must contain relative formatted local date") {
                    checkAll(
                        LocaleData().allLocales.exhaustive(),
                        Arb.localDateTime().map { it.toKotlinLocalDateTime() },
                        Arb.kotlinInstant(),
                        Arb.zoneId().map { it.toKotlinTimeZone() },
                        Arb.enum<RelativeDateTimeFormatter.Style>(),
                    ) { locale, dateTime, now, tz, relativeStyle ->
                        val dateString = RelativeLocalDateFormatter(locale, style = relativeStyle).format(dateTime.date, now, tz).value
                        val formatted =
                            RelativeDateAbsoluteTimeFormatter(locale, relativeStyle = relativeStyle).format(dateTime, now, tz).value
                        formatted shouldContain dateString
                    }
                }
            }

            context("nextTick") {
                val formatter = RelativeDateAbsoluteTimeFormatter(ULocale.ENGLISH)
                test("is always identical to RelativeLocalDateFormatter (as time is always absolute)") {
                    val dateFormatter = RelativeLocalDateFormatter(ULocale.ENGLISH)
                    checkAll(
                        Arb.localDateTime().map { it.toKotlinLocalDateTime() },
                        Arb.kotlinInstant(),
                        Arb.zoneId().map { it.toKotlinTimeZone() },
                    ) { localDateTime, now, zoneId ->
                        formatter.format(localDateTime, now, zoneId).nextTick shouldBe dateFormatter.format(
                            localDateTime.date,
                            now,
                            zoneId,
                        ).nextTick
                    }
                }

                nextTickPredictsChangeTest(
                    arb = Arb.localDateTime().map { it.toKotlinLocalDateTime() },
                    valueFromInstant = { instant, tz -> instant.toLocalDateTime(tz) },
                    format = { dateTime, now, tz -> formatter.format(dateTime, now, tz) },
                )
            }
        }
    },
)

private fun RelativeDateAbsoluteTimeFormatter.format(diff: Duration): TickingValue<String> {
    return format((NOW + diff).toLocalDateTime(TZ), NOW, TZ)
}
