package io.github.couchtracker.db.user.model.partialtime

import io.github.couchtracker.testComparables
import io.kotest.core.spec.style.FunSpec
import kotlinx.datetime.Month

class PartialDateTimeGroupSortTest : FunSpec(
    {
        context("compareBy()") {
            testComparables(
                name = "Base test",
                // Unknown must be before anything else
                PartialDateTimeGroup.Unknown,

                // Year must be before YearMonth
                PartialDateTimeGroup.Year(PartialDateTime.Local.Year(2023)),

                // Months must be sorted
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2023, Month.JANUARY)),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2023, Month.JUNE)),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2023, Month.DECEMBER)),

                PartialDateTimeGroup.Year(PartialDateTime.Local.Year(2024)),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2024, Month.FEBRUARY)),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2024, Month.MARCH)),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2024, Month.OCTOBER)),
            )
        }
    },
)
