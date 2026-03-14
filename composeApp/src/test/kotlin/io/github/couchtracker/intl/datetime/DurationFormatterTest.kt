package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.ULocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class DurationFormatterTest : FunSpec(
    {
        context("format") {
            context("fails with invalid durations") {
                withTests(
                    Duration.INFINITE,
                    -1.nanoseconds,
                    -10.minutes,
                ) { duration ->
                    val formatter = DurationFormatter(
                        locale = ULocale.US,
                        formatWidth = FormatWidth.SHORT,
                        omitZeros = true,
                        minUnit = DurationUnit.MILLISECONDS,
                        maxUnits = 1,
                    )
                    shouldThrow<IllegalArgumentException> {
                        formatter.format(duration)
                    }
                }
            }

            context("works") {
                // These are just some basic smoke tests, testing mainly formatting behavior of ICU rather than other custom format options,
                // as those are thoroughly tested by UnitsFormatter's tests, which this class relies upon

                val duration = 3.hours + 25.seconds + 250.milliseconds
                withTests(
                    nameFn = { it.toString() },
                    tuple(ULocale.ENGLISH, FormatWidth.WIDE, "3 hours, 25 seconds"),
                    tuple(ULocale.ENGLISH, FormatWidth.SHORT, "3 hr, 25 sec"),
                    tuple(ULocale.ENGLISH, FormatWidth.NARROW, "3h 25s"),
                    tuple(ULocale.ENGLISH, FormatWidth.NUMERIC, "3:00:25"),
                    tuple(ULocale.ITALIAN, FormatWidth.WIDE, "3 ore e 25 secondi"),
                ) { (locale, formatWidth, expected) ->
                    val formatter = DurationFormatter(
                        locale = locale,
                        formatWidth = formatWidth,
                        omitZeros = true,
                        minUnit = DurationUnit.SECONDS,
                        maxUnits = 3,
                    )
                    formatter.format(duration) shouldBe expected
                }
            }
        }
    },
)
