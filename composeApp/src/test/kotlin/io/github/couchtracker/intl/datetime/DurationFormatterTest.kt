package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.ULocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
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
                        omitZeros = true,
                        minUnit = DurationUnit.MILLISECONDS,
                        maxUnits = 1,
                        measureFormat = mockk(),
                    )
                    shouldThrow<IllegalArgumentException> {
                        formatter.format(duration)
                    }
                }
            }

            test("works") {
                // This is just a smoke test, more in-depth test is done by UnitsFormatterTest, which is the underlying class used

                val formatter = DurationFormatter(
                    omitZeros = true,
                    minUnit = DurationUnit.SECONDS,
                    maxUnits = 3,
                    measureFormat = MeasureFormat.getInstance(ULocale.US, MeasureFormat.FormatWidth.WIDE),
                )

                formatter.format(3.days + 24.minutes + 25.seconds) shouldBe "3 days, 24 minutes"
            }
        }
    },
)
