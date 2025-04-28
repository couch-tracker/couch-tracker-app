package io.github.couchtracker.db.profile.model.partialtime

import io.github.couchtracker.testComparables
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlinx.datetime.Month

class PartialDateTimeSortTest : FunSpec(
    {
        context("compareBy()") {
            context("Local") {
                testComparables(
                    name = "Local years are sorted by year",
                    PartialDateTime.Local.parse("2024"),
                    PartialDateTime.Local.parse("2025"),
                    PartialDateTime.Local.parse("2026"),
                )
                testComparables(
                    name = "Local year months are sorted by year and month",
                    PartialDateTime.Local.parse("2024-01"),
                    PartialDateTime.Local.parse("2024-02"),
                    PartialDateTime.Local.parse("2025"),
                )
                testComparables(
                    name = "Local dates are sorted by date",
                    PartialDateTime.Local.parse("2024-01-01"),
                    PartialDateTime.Local.parse("2024-01-02"),
                    PartialDateTime.Local.parse("2024-02-01"),
                    PartialDateTime.Local.parse("2025-01-01"),
                )
                testComparables(
                    name = "Local times are sorted by date and time",
                    PartialDateTime.Local.parse("2024-01-01T00:00:00"),
                    PartialDateTime.Local.parse("2024-01-01T00:00:01"),
                    PartialDateTime.Local.parse("2024-01-01T00:01:00"),
                    PartialDateTime.Local.parse("2024-01-01T01:00:00"),
                    PartialDateTime.Local.parse("2024-01-02T00:00:00"),
                    PartialDateTime.Local.parse("2024-02-02T00:00:00"),
                    PartialDateTime.Local.parse("2025-02-02T00:00:00"),
                )
                testComparables(
                    name = "Local values for the same instant are sorted according to precision",
                    PartialDateTime.Local.parse("2024"),
                    PartialDateTime.Local.parse("2024-01"),
                    PartialDateTime.Local.parse("2024-01-01"),
                    PartialDateTime.Local.parse("2024-01-01T00:00:00"),
                )
            }

            context("Zoned") {
                testComparables(
                    name = "Zoned years are sorted according to year and timezone",
                    PartialDateTime.Zoned.parse("2024+10:00"),
                    PartialDateTime.Zoned.parse("2024+00:00"),
                    PartialDateTime.Zoned.parse("2024-10:00"),
                    PartialDateTime.Zoned.parse("2025+18:00"),
                )

                testComparables(
                    name = "Zoned year months are sorted according to year, month and timezone",
                    PartialDateTime.Zoned.parse("2024-01+10:00"),
                    PartialDateTime.Zoned.parse("2024-01+00:00"),
                    PartialDateTime.Zoned.parse("2024-01-10:00"),
                    PartialDateTime.Zoned.parse("2024-02+18:00"),
                    PartialDateTime.Zoned.parse("2025-02+18:00"),
                )

                testComparables(
                    name = "Zoned dates are sorted sorted based on their instants, even if their local parts would be swapped",
                    PartialDateTime.Zoned.parse("2024-01-01+10:00"),
                    PartialDateTime.Zoned.parse("2024-01-01+00:00"),
                    PartialDateTime.Zoned.parse("2024-01-02+18:00"),
                    PartialDateTime.Zoned.parse("2024-01-01-10:00"),
                    PartialDateTime.Zoned.parse("2024-02-01+18:00"),
                    PartialDateTime.Zoned.parse("2025-01-01+18:00"),
                )

                testComparables(
                    name = "Zoned values are sorted based on their instants, even if their local part would be swapped",
                    PartialDateTime.Zoned.parse("2024-01-01T12:00:00+05:00"),
                    PartialDateTime.Zoned.parse("2024-01-01T10:00:00+00:00"),

                    PartialDateTime.Zoned.parse("2024-02-02T12:00:00[Europe/Dublin]"), // +00:00
                    PartialDateTime.Zoned.parse("2024-02-02T10:00:00[Pacific/Honolulu]"), // -10:00
                )

                testComparables(
                    name = "Zoned values for same instant and same timezone are sorted according to precision",
                    PartialDateTime.Zoned.parse("2024+05:00"),
                    PartialDateTime.Zoned.parse("2024-01+05:00"),
                    PartialDateTime.Zoned.parse("2024-01-01+05:00"),
                    PartialDateTime.Zoned.parse("2024-01-01T00:00:00+05:00"),
                )

                // This test is interesting: the toInstant() of these values would sort them in a different order, but since they have
                // low precision, we choose to sort them based on their local part and precision
                testComparables(
                    name = "Zoned values up to YearMonth are sorted according to precision, even if their instants would be swapped",
                    PartialDateTime.Zoned.parse("2024-18:00"),
                    PartialDateTime.Zoned.parse("2024-01-10:00"),
                    PartialDateTime.Zoned.parse("2024-01-01+00:00"),

                    PartialDateTime.Zoned.parse("2025-18:00"),
                    PartialDateTime.Zoned.parse("2025-01-10:00"),
                    PartialDateTime.Zoned.parse("2025-01-01+00:00"),
                )

                testComparables(
                    name = "Zone dates and date times are sorted according to instant, not according to precisions",
                    PartialDateTime.Zoned.parse("2024-01-01T00:00:00+05:00"),
                    PartialDateTime.Zoned.parse("2024-01-01+00:00"),
                )

                // DST changes should make differences in how values are sorted, making the two timezones possibly "flip" at different dates
                testComparables(
                    name = "Zoned values are sorted also based on DST observance",
                    // America/Phoenix DOESN'T observe DST, and is always at -07:00
                    // America/Boise DOES: SDT −07:00, DST −06:00

                    // when out of DST, they are at the same offset (-07:00)
                    PartialDateTime.Zoned.parse("2024-03-09T10:00:00[America/Phoenix]"),
                    PartialDateTime.Zoned.parse("2024-03-09T10:30:00[America/Boise]"),

                    // DST change for 2024 in US is Match 10th

                    // when in DST, America/Boise goes to -06:00
                    PartialDateTime.Zoned.parse("2024-03-10T10:30:00[America/Boise]"),
                    PartialDateTime.Zoned.parse("2024-03-10T10:00:00[America/Phoenix]"),
                )
            }
        }

        context("sort()") {
            withData(
                mapOf(
                    // In the case of mixed Local and Zoned date times, the Local goes first
                    "Local values always go first than Zoned values" to listOf(
                        PartialDateTime.parse("2024-01-01"),
                        PartialDateTime.parse("2024-01-01+18:00"),

                        PartialDateTime.parse("2024-01-01T10:00:00"),
                        PartialDateTime.parse("2024-01-01T12:00:00+18:00"),
                    ),

                    // This example, other than testing all possible types together, also has a peculiar edge case:
                    // `2023-12-31T23:59:59-18:00` and `2024-01-01T00:00-18:00` are only one second apart, yet they appear at very different positions
                    // This is because they are in different local years, and sorting gives priority to the local part most of the time
                    "All types example" to listOf(
                        PartialDateTime.parse("2023-12-31T23:59:59-18:00"),

                        PartialDateTime.parse("2024"),
                        PartialDateTime.parse("2024+10:00"),
                        PartialDateTime.parse("2024-10:00"),

                        PartialDateTime.parse("2024-01"),
                        PartialDateTime.parse("2024-01+10:00"),
                        PartialDateTime.parse("2024-01-10:00"),

                        PartialDateTime.parse("2024-01-01"),
                        PartialDateTime.parse("2024-01-01T00:00"),

                        PartialDateTime.parse("2024-01-01T00:00+18:00"),
                        PartialDateTime.parse("2024-01-01+10:00"),
                        PartialDateTime.parse("2024-01-01-10:00"),
                        PartialDateTime.parse("2024-01-01T00:00-18:00"),

                        PartialDateTime.parse("2024-01-01T00:02"),

                        PartialDateTime.parse("2024-01-05T00:00"),
                        PartialDateTime.parse("2024-01-05T00:00+18:00"),
                        PartialDateTime.parse("2024-01-05T00:00-18:00"),

                        PartialDateTime.parse("2024-02-01"),
                        PartialDateTime.parse("2024-02-01[Asia/Shanghai]"),
                        PartialDateTime.parse("2024-02-01T00:00"),
                        PartialDateTime.parse("2024-02-01T00:00[Europe/Rome]"),
                        PartialDateTime.parse("2024-02-01[America/New_York]"),
                        PartialDateTime.parse("2024-02-01T00:00[Pacific/Honolulu]"),

                    ),
                ),
            ) { items ->
                items.shuffled().sort() shouldBe items
            }
        }

        test("groupAndSort()") {
            data class TestItem(
                val name: String,
                val date: PartialDateTime,
            )

            val a = TestItem(name = "aaa", date = PartialDateTime.parse("2023-12-31T23:59:59-18:00"))
            val b = TestItem(name = "bbb", date = PartialDateTime.parse("2024"))
            val c = TestItem(name = "ccc", date = PartialDateTime.parse("2024-01"))
            val d = TestItem(name = "ddd", date = PartialDateTime.parse("2024-01-01"))
            val e = TestItem(name = "eee", date = PartialDateTime.parse("2024-01-01T12:34:56Z"))
            val f = TestItem(name = "fff", date = PartialDateTime.parse("2024-01-01T12:34:56Z"))

            val result = PartialDateTime.sortAndGroup(
                items = listOf(a, b, c, d, e, f).shuffled(),
                getPartialDateTime = { date },
                additionalComparator = compareBy { it.name },
            )

            result shouldBe mapOf(
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2023, Month.DECEMBER)) to listOf(a),
                PartialDateTimeGroup.Year(PartialDateTime.Local.Year(2024)) to listOf(b),
                PartialDateTimeGroup.YearMonth(PartialDateTime.Local.YearMonth(2024, Month.JANUARY)) to listOf(c, d, e, f),
            )
        }
    },
)
