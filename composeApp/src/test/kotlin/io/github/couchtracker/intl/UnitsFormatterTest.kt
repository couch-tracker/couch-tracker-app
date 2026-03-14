package io.github.couchtracker.intl

import com.ibm.icu.text.MeasureFormat.FormatWidth
import com.ibm.icu.util.MeasureUnit
import com.ibm.icu.util.TimeUnit
import com.ibm.icu.util.ULocale
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withContexts
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import kotlinx.datetime.DateTimePeriod
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * This test is ONLY testing the behaviors of the various configuration options, namely: locale, formatWidth, omitZeros, minUnit, maxUnits.
 *
 * The fact that we are testing a DateTimePeriod is not relevant, it's just used as a vehicle to test the above.
 */
class UnitsFormatterTest : FunSpec(
    {
        context("constructor") {
            context("maxUnits must be positive") {
                withTests(-10, -2, -1, 0) { maxUnits ->
                    shouldThrow<IllegalArgumentException> {
                        testFormatter(
                            omitZeros = true,
                            minUnit = DateTimePeriodUnit.MILLISECONDS,
                            maxUnits = maxUnits,
                        )
                    }
                }
            }
        }
        context("format") {
            context("works") {
                data class TestCase(
                    val omitZeros: Boolean,
                    val minUnit: DateTimePeriodUnit,
                    val maxUnits: Int,
                    val period: DateTimePeriod,
                    val expected: String,
                ) {
                    fun run() {
                        val formatter = testFormatter(
                            omitZeros = omitZeros,
                            minUnit = minUnit,
                            maxUnits = maxUnits,
                        )
                        formatter.format(period) shouldBe expected
                    }
                }

                suspend fun FunSpecContainerScope.runTestCases(
                    omitZeros: List<Boolean>,
                    minUnit: List<DateTimePeriodUnit>,
                    maxUnits: List<Int>,
                    period: DateTimePeriod,
                    expected: String,
                ) {
                    checkAll(omitZeros.exhaustive(), minUnit.exhaustive(), maxUnits.exhaustive()) { omitZeros, minUnit, maxUnits ->
                        val testCase = TestCase(
                            omitZeros = omitZeros,
                            minUnit = minUnit,
                            maxUnits = maxUnits,
                            period = period,
                            expected = expected,
                        )
                        test(testCase.toString()) {
                            testCase.run()
                        }
                    }
                }

                context("maxUnits is respected") {
                    context("maxUnit = 2") {
                        runTestCases(
                            omitZeros = listOf(true, false),
                            minUnit = listOf(DateTimePeriodUnit.MINUTES, DateTimePeriodUnit.SECONDS, DateTimePeriodUnit.MILLISECONDS),
                            maxUnits = listOf(2),
                            period = DateTimePeriod(hours = 5, minutes = 3, seconds = 10, nanoseconds = 150_000_000),
                            expected = "5 hours, 3 minutes",
                        )
                    }
                    context("maxUnit = 1") {
                        runTestCases(
                            omitZeros = listOf(true, false),
                            minUnit = listOf(DateTimePeriodUnit.HOURS, DateTimePeriodUnit.MINUTES, DateTimePeriodUnit.SECONDS),
                            maxUnits = listOf(1),
                            period = DateTimePeriod(hours = 5, minutes = 10, seconds = 40),
                            expected = "5 hours",
                        )
                    }
                    context("omitted zero units are considered for maxUnit") {
                        runTestCases(
                            omitZeros = listOf(true),
                            minUnit = listOf(
                                DateTimePeriodUnit.MINUTES,
                                DateTimePeriodUnit.SECONDS,
                                DateTimePeriodUnit.MILLISECONDS,
                                DateTimePeriodUnit.NANOSECONDS,
                            ),
                            maxUnits = listOf(3),
                            period = DateTimePeriod(hours = 5, nanoseconds = 150_000_000),
                            expected = "5 hours",
                        )
                    }
                }

                context("enabled omit zeros avoids 0 middle units") {
                    runTestCases(
                        omitZeros = listOf(true),
                        minUnit = listOf(DateTimePeriodUnit.MILLISECONDS, DateTimePeriodUnit.NANOSECONDS),
                        maxUnits = listOf(5),
                        period = DateTimePeriod(hours = 5, nanoseconds = 250_000_000),
                        expected = "5 hours, 250 milliseconds",
                    )
                }

                context("disabled omit zeros will make relevant unit show up as zero") {
                    runTestCases(
                        omitZeros = listOf(false),
                        minUnit = listOf(DateTimePeriodUnit.MINUTES, DateTimePeriodUnit.SECONDS),
                        maxUnits = listOf(2),
                        period = DateTimePeriod(hours = 1, nanoseconds = 150_000_000),
                        expected = "1 hour, 0 minutes",
                    )
                }

                context("minUnit higher than duration causes zero min unit output") {
                    runTestCases(
                        omitZeros = listOf(true, false),
                        minUnit = listOf(DateTimePeriodUnit.MINUTES),
                        maxUnits = listOf(1, 2),
                        period = DateTimePeriod(seconds = 59),
                        expected = "0 minutes",
                    )
                }

                context("zero duration outputs 0 with min unit") {
                    runTestCases(
                        omitZeros = listOf(true, false),
                        minUnit = listOf(DateTimePeriodUnit.MINUTES),
                        maxUnits = listOf(1, 2, 3),
                        period = DateTimePeriod(0),
                        expected = "0 minutes",
                    )
                    runTestCases(
                        omitZeros = listOf(true, false),
                        minUnit = listOf(DateTimePeriodUnit.HOURS),
                        maxUnits = listOf(1, 2, 3),
                        period = DateTimePeriod(0),
                        expected = "0 hours",
                    )
                }

                context("format width is applied") {
                    withTests(
                        FormatWidth.NARROW to "1h 5m",
                        FormatWidth.SHORT to "1 hr, 5 min",
                        FormatWidth.WIDE to "1 hour, 5 minutes",
                    ) { (formatWidth, expected) ->
                        val formatter = testFormatter(
                            formatWidth = formatWidth,
                            omitZeros = true,
                            minUnit = DateTimePeriodUnit.SECONDS,
                            maxUnits = 2,
                        )
                        formatter.format(DateTimePeriod(hours = 1, minutes = 5)) shouldBe expected
                    }
                }

                context("locale is applied") {
                    withTests(
                        nameFn = { it.toString() },
                        ULocale.ENGLISH to "1 hour, 5 minutes",
                        ULocale.ITALIAN to "1 ora e 5 minuti",
                        ULocale("ga") to "1 u agus 5 nóim",
                    ) { (locale, expected) ->
                        val formatter = testFormatter(
                            omitZeros = true,
                            minUnit = DateTimePeriodUnit.SECONDS,
                            maxUnits = 2,
                            locale = locale,
                        )
                        formatter.format(DateTimePeriod(hours = 1, minutes = 5)) shouldBe expected
                    }
                }
            }
        }

        context("units") {
            test("no more than maxUnit units are returned") {
                val formatter = testFormatter(
                    omitZeros = true,
                    minUnit = DateTimePeriodUnit.MILLISECONDS,
                    maxUnits = 3,
                )
                formatter.units(DateTimePeriod(hours = 5, seconds = 15)) shouldBe listOf(
                    DateTimePeriodUnit.HOURS,
                    DateTimePeriodUnit.MINUTES,
                    DateTimePeriodUnit.SECONDS,
                )
            }

            test("not enough units to fulfill max units") {
                val formatter = testFormatter(
                    omitZeros = true,
                    minUnit = DateTimePeriodUnit.MINUTES,
                    maxUnits = 2,
                )
                formatter.units(DateTimePeriod(minutes = 40)) shouldBe listOf(
                    DateTimePeriodUnit.MINUTES,
                )
            }

            test("period is smaller than 1 minUnit") {
                val formatter = testFormatter(
                    omitZeros = true,
                    minUnit = DateTimePeriodUnit.HOURS,
                    maxUnits = 2,
                )
                formatter.units(DateTimePeriod(minutes = 59, seconds = 59)) shouldBe listOf(
                    DateTimePeriodUnit.HOURS,
                )
            }

            context("a zero period returns a single unit equal to minUnit") {
                withContexts(DateTimePeriodUnit.entries) { minUnit ->
                    withTests(nameFn = { "maxUnits=$it" }, 1, 2, 3) { maxUnits ->
                        val formatter = testFormatter(
                            omitZeros = true,
                            minUnit = minUnit,
                            maxUnits = maxUnits,
                        )
                        formatter.units(DateTimePeriod()) shouldBe listOf(minUnit)
                    }
                }
            }
        }
    },
)

private fun testFormatter(
    omitZeros: Boolean,
    minUnit: DateTimePeriodUnit,
    maxUnits: Int,
    locale: ULocale = ULocale.US,
    formatWidth: FormatWidth = FormatWidth.WIDE,
) = UnitsFormatter<DateTimePeriod, DateTimePeriodUnit>(
    locale = locale,
    formatWidth = formatWidth,
    omitZeros = omitZeros,
    minUnit = minUnit,
    maxUnits = maxUnits,
    unitPart = { unitPart(it) },
    ibmIcuUnit = { ibmIcuUnit },
)

private enum class DateTimePeriodUnit(val ibmIcuUnit: MeasureUnit) {
    NANOSECONDS(TimeUnit.NANOSECOND),
    MICROSECONDS(TimeUnit.MICROSECOND),
    MILLISECONDS(TimeUnit.MILLISECOND),
    SECONDS(TimeUnit.SECOND),
    MINUTES(TimeUnit.MINUTE),
    HOURS(TimeUnit.HOUR),
    DAYS(TimeUnit.DAY),
    MONTHS(TimeUnit.MONTH),
    YEARS(TimeUnit.YEAR),
}

private fun DateTimePeriod.unitPart(unit: DateTimePeriodUnit) = when (unit) {
    DateTimePeriodUnit.NANOSECONDS -> (nanoseconds % 1_000).toLong()
    DateTimePeriodUnit.MICROSECONDS -> ((nanoseconds / 1000) % 1000).toLong()
    DateTimePeriodUnit.MILLISECONDS -> nanoseconds / 1_000_000L
    DateTimePeriodUnit.SECONDS -> seconds.toLong()
    DateTimePeriodUnit.MINUTES -> minutes.toLong()
    DateTimePeriodUnit.HOURS -> hours.toLong()
    DateTimePeriodUnit.DAYS -> days.toLong()
    DateTimePeriodUnit.MONTHS -> months.toLong()
    DateTimePeriodUnit.YEARS -> years.toLong()
}
