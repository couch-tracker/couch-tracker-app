@file:Suppress("DEPRECATION")

package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.TickingValue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.duration
import io.kotest.property.checkAll
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class RelativeDurationFormatterTest : FunSpec(
    {
        context("format") {
            // These are just basic smoke tests, as thorough testing is already carried out in DurationFormatter and UnitsFormatter tests
            // for the format and remainderUntilNextUnitBoundary for the next tick
            context("basic tests") {
                withTests(
                    nameFn = { Pair(it.b, it.c).toString() },
                    tuple(
                        RelativeDurationFormatter(ULocale.ENGLISH, FormatWidth.WIDE),
                        1.days + 5.hours,
                        TickingValue("1 day, 5 hours", nextTick = 1.nanoseconds),
                    ),
                    tuple(
                        RelativeDurationFormatter(ULocale.ENGLISH, FormatWidth.NARROW),
                        5.hours + 15.minutes + 43.seconds,
                        TickingValue("5h 15m", nextTick = 43.seconds + 1.nanoseconds),
                    ),
                    tuple(
                        RelativeDurationFormatter(ULocale.ITALIAN, FormatWidth.WIDE),
                        -(6.hours + 13.seconds),
                        TickingValue("6 ore", nextTick = 47.seconds),
                    ),
                    tuple(
                        RelativeDurationFormatter(ULocale.ENGLISH, FormatWidth.NARROW, minUnit = DurationUnit.SECONDS),
                        -(4.minutes + 22.seconds + 440.milliseconds + 141.microseconds),
                        TickingValue("4m 22s", nextTick = 559.milliseconds + 859.microseconds),
                    ),
                ) { (formatter, duration, expected) ->
                    formatter.format(duration) shouldBe expected
                }
            }

            context("formatted string") {
                test("negative duration yields same format as positive") {
                    val formatter = RelativeDurationFormatter(ULocale.ENGLISH)
                    checkAll(Arb.duration()) { duration ->
                        formatter.format(duration).value shouldBe formatter.format(-duration).value
                    }
                }
            }

            context("nextTick") {
                test("should never be null and always non-finite positive") {
                    val formatter = RelativeDurationFormatter(ULocale.ENGLISH)
                    checkAll(Arb.duration()) { duration ->
                        formatter.format(duration).nextTick.shouldNotBeNull() should {
                            it.isPositive().shouldBeTrue()
                            it.isFinite().shouldBeTrue()
                        }
                    }
                }
            }
        }
    },
)
