package io.github.couchtracker.db.profile.model.partialtime

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

class PartialDateTimeTest : FunSpec(
    {
        context("Local") {
            context("atZone()") {
                withData(
                    nameFn = { it.serialize() },
                    PartialDateTime.parse("2024"),
                    PartialDateTime.parse("2024-07"),
                    PartialDateTime.parse("2024-07-01"),
                    PartialDateTime.parse("2024-07-01T22:12:34"),
                ) { local ->
                    local as PartialDateTime.Local

                    withData(
                        nameFn = { it.toString() },
                        TimeZone.UTC,
                        TimeZone.of("Europe/Dublin"),
                        TimeZone.of("America/New_York"),
                        TimeZone.of("Australia/Sydney"),
                        TimeZone.of("Australia/Sydney"),
                        FixedOffsetTimeZone(UtcOffset(hours = -10)),
                        FixedOffsetTimeZone(UtcOffset(hours = +10)),
                    ) { timeZone ->
                        val zoned = local.atZone(timeZone)

                        zoned.local shouldBe local
                        zoned.zone shouldBe timeZone
                    }
                }
            }
            context("toInstant()") {
                val grouped = buildInstantTestCases().groupBy { it.local }
                withData(nameFn = { it.key.serialize() }, grouped.entries) { (_, cases) ->
                    withData(nameFn = { "at timezone ${it.timeZone}" }, cases) { (local, timeZone, expected) ->
                        local.toInstant(timeZone) shouldBe expected
                    }
                }
            }
        }
        context("Zoned") {
            context("toInstant()") {
                val tests = buildInstantTestCases().map { it.local.atZone(it.timeZone) to it.expected }
                withData(nameFn = { it.first.serialize() }, tests) { (zoned, expected) ->
                    zoned.toInstant() shouldBe expected
                }
            }
        }
    },
)

private data class ToInstantTestCase(
    val local: PartialDateTime.Local,
    val timeZone: TimeZone,
    val expected: Instant,
)

private fun buildInstantTestCases(): List<ToInstantTestCase> {
    val localValues = listOf(
        PartialDateTime.parse("2024"),
        PartialDateTime.parse("2024-01"),
        PartialDateTime.parse("2024-01-01"),
        PartialDateTime.parse("2024-01-01T00:00:00"),
    )
    val baseTestCases = localValues.flatMap {
        listOf(
            ToInstantTestCase(
                local = it as PartialDateTime.Local,
                timeZone = FixedOffsetTimeZone(UtcOffset(hours = +10)),
                expected = Instant.parse("2023-12-31T14:00:00Z"),
            ),
            ToInstantTestCase(
                local = it,
                timeZone = TimeZone.of("Europe/Rome"),
                expected = Instant.parse("2023-12-31T23:00:00Z"),
            ),
            ToInstantTestCase(
                local = it,
                timeZone = TimeZone.UTC,
                expected = Instant.parse("2024-01-01T00:00:00Z"),
            ),
            ToInstantTestCase(
                local = it,
                timeZone = FixedOffsetTimeZone(UtcOffset(hours = -10)),
                expected = Instant.parse("2024-01-01T10:00:00Z"),
            ),
        )
    }
    return baseTestCases + listOf(
        // This also validates that on a timezone with DST we get the correct instant
        ToInstantTestCase(
            local = PartialDateTime.parse("2024-07-01T12:34:56") as PartialDateTime.Local,
            timeZone = TimeZone.of("Europe/Rome"),
            expected = Instant.parse("2024-07-01T10:34:56Z"),
        ),
    )
}
