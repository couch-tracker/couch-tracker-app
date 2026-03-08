package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DateTimePeriod

class DateTimePeriodTest : FunSpec(
    {
        context("isAnyComponentNegative") {
            withTests(
                nameFn = { it.first.toString() },
                // Zero time period
                DateTimePeriod() to false,

                // Normalized zero date time periods
                DateTimePeriod(years = 2, months = -24) to false,
                DateTimePeriod(hours = 3, minutes = -180) to false,

                // Single negative date component
                DateTimePeriod(years = -1) to true,
                DateTimePeriod(months = -4) to true,

                // Single negative time component
                DateTimePeriod(minutes = -15) to true,
                DateTimePeriod(nanoseconds = -1) to true,

                // Multiple negative components
                DateTimePeriod(years = -1, months = -2, days = -3, hours = -4, minutes = -5) to true,

                // Normalized positive date component
                DateTimePeriod(years = 1, months = -1) to false,

                // Normalized positive time component
                DateTimePeriod(hours = -2, minutes = 150) to false,
                DateTimePeriod(minutes = 50, seconds = -200) to false,

                // Normalized negative date component
                DateTimePeriod(years = -1, months = 5) to true,

                // Normalized negative time component
                DateTimePeriod(hours = 2, minutes = -300) to true,
                DateTimePeriod(minutes = -50, seconds = 100) to true,

                // Multiple positive components
                DateTimePeriod(years = 1, months = 2, days = 3, hours = 4, minutes = 5) to false,

                // Negative date, positive time
                DateTimePeriod(months = -1, hours = 10_000) to true,

                // Positive date, negative time
                DateTimePeriod(years = 1, minutes = -1) to true,
            ) { (period, expected) ->
                period.isAnyComponentNegative() shouldBe expected
            }
        }
        context("unitPart") {
            val period = DateTimePeriod(
                years = 12,
                months = 2,
                days = 9,
                hours = 13,
                minutes = 40,
                seconds = 21,
                nanoseconds = 320_255_420,
            )
            withData(
                DateTimePeriodUnit.YEARS to 12,
                DateTimePeriodUnit.MONTHS to 2,
                DateTimePeriodUnit.DAYS to 9,
                DateTimePeriodUnit.HOURS to 13,
                DateTimePeriodUnit.MINUTES to 40,
                DateTimePeriodUnit.SECONDS to 21,
                DateTimePeriodUnit.MILLISECONDS to 320,
                DateTimePeriodUnit.MICROSECONDS to 255,
                DateTimePeriodUnit.NANOSECONDS to 420,
            ) { (unit, expected) ->
                period.unitPart(unit) shouldBe expected.toLong()
            }

            context("zero duration") {
                withData(DateTimePeriodUnit.entries) { unit ->
                    DateTimePeriod(0).unitPart(unit) shouldBe 0
                }
            }
        }
    },
)
