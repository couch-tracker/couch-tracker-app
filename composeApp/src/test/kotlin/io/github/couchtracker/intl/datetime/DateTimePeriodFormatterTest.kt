package io.github.couchtracker.intl.datetime

import com.ibm.icu.text.MeasureFormat
import com.ibm.icu.util.ULocale
import io.github.couchtracker.utils.DateTimePeriodUnit
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import kotlinx.datetime.DateTimePeriod

class DateTimePeriodFormatterTest : FunSpec(
    {
        context("format") {
            context("fails with invalid values") {
                val formatter = DateTimePeriodFormatter(
                    omitZeros = true,
                    minUnit = DateTimePeriodUnit.MILLISECONDS,
                    maxUnits = 2,
                    measureFormat = mockk(),
                )

                withData(
                    DateTimePeriod(minutes = -1),
                    DateTimePeriod(nanoseconds = -1),
                ) {
                    shouldThrow<IllegalArgumentException> {
                        formatter.format(it)
                    }
                }
            }

            test("works") {
                // This is just a smoke test, more in-depth test is done by UnitsFormatterTest, which is the underlying class used

                val formatter = DateTimePeriodFormatter(
                    omitZeros = true,
                    minUnit = DateTimePeriodUnit.SECONDS,
                    maxUnits = 3,
                    measureFormat = MeasureFormat.getInstance(ULocale.US, MeasureFormat.FormatWidth.WIDE),
                )

                formatter.format(DateTimePeriod(hours = 1, seconds = 24, nanoseconds = 250_123_345)) shouldBe "1 hour, 24 seconds"
            }
        }
    },
)
