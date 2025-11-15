package io.github.couchtracker.db.profile.model.partialtime

import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime.Local
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime.Zoned
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.engine.names.WithDataTestName
import io.kotest.matchers.shouldBe
import kotlinx.datetime.FixedOffsetTimeZone
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.UtcOffset

private val LOCAL_TEST_CASES = listOf(
    ValidTestCase(
        testName = "Year",
        serialized = "2024",
        value = Local.Year(2024),
    ),
    ValidTestCase(
        testName = "Year and Month",
        serialized = "2024-07",
        value = Local.YearMonth(2024, Month.JULY),
    ),
    ValidTestCase(
        testName = "Date",
        serialized = "2024-07-01",
        value = Local.Date(LocalDate(2024, Month.JULY, day = 1)),
    ),
    ValidTestCase(
        testName = "Date and time at hour mark",
        serialized = "2024-07-01T08:00:00",
        otherAcceptedParsableFormats = listOf("2024-07-01T08:00"),
        value = Local.DateTime(LocalDateTime(2024, Month.JULY, day = 1, hour = 8, minute = 0)),
    ),
    ValidTestCase(
        testName = "Date and time with minutes and seconds",
        serialized = "2024-07-01T22:12:34",
        value = Local.DateTime(LocalDateTime(2024, Month.JULY, day = 1, hour = 22, minute = 12, second = 34)),
    ),
)

private val ZONED_TEST_CASES = LOCAL_TEST_CASES.flatMap { tc ->
    val local = tc.value as Local
    listOf(
        tc.copy(
            testName = "${tc.testName} with timezone ID",
            serialized = "${tc.serialized}[Europe/Rome]",
            otherAcceptedParsableFormats = tc.otherAcceptedParsableFormats.map { "$it[Europe/Rome]" },
            value = Zoned(local, TimeZone.of("Europe/Rome")),
        ),
        tc.copy(
            testName = "${tc.testName} with negative timezone offset",
            serialized = "${tc.serialized}-05:45",
            otherAcceptedParsableFormats = tc.otherAcceptedParsableFormats.map { "$it-05:45" },
            value = Zoned(local, FixedOffsetTimeZone(UtcOffset(hours = -5, minutes = -45))),
        ),
        tc.copy(
            testName = "${tc.testName} with positive timezone offset with seconds",
            serialized = "${tc.serialized}+10:00:05",
            otherAcceptedParsableFormats = tc.otherAcceptedParsableFormats.map { "$it+10:00:05" },
            value = Zoned(local, FixedOffsetTimeZone(UtcOffset(hours = 10, minutes = 0, seconds = 5))),
        ),
        tc.copy(
            testName = "${tc.testName} with Z offset",
            serialized = "${tc.serialized}Z",
            otherAcceptedParsableFormats = tc.otherAcceptedParsableFormats.map { "${it}Z" },
            value = Zoned(local, FixedOffsetTimeZone(UtcOffset.ZERO)),
        ),
    )
}

private data class ValidTestCase(
    val serialized: String,
    val otherAcceptedParsableFormats: List<String> = emptyList(),
    val value: PartialDateTime,
    val testName: String,
) : WithDataTestName {
    override fun dataTestName() = "$testName ($serialized)"
}

private data class InvalidTestCase(
    val value: String,
    val testName: String,
) : WithDataTestName {
    override fun dataTestName() = "$testName ($value)"
}

class PartialDateTimeSerializationTest : FunSpec(
    {
        val validTestCases = LOCAL_TEST_CASES + ZONED_TEST_CASES

        context("serialize()") {
            withData(validTestCases) { tc ->
                tc.value.serialize() shouldBe tc.serialized
            }
        }

        context("parse()") {
            context("works for valid cases") {
                withData(validTestCases) { tc ->
                    withData(
                        (listOf(tc.serialized) + tc.otherAcceptedParsableFormats).withIndex().associate { (i, format) ->
                            (if (i == 0) "main" else "additional") + " format ($format)" to format
                        },
                    ) {
                        PartialDateTime.parse(it) shouldBe tc.value
                    }
                }
            }
        }
        context("fails for invalid formats") {
            withData(
                InvalidTestCase(
                    testName = "empty string",
                    value = "",
                ),
                InvalidTestCase(
                    testName = "blank string",
                    value = "   ",
                ),
                InvalidTestCase(
                    testName = "truncated date",
                    value = "2024-",
                ),
                InvalidTestCase(
                    testName = "non trimmed year",
                    value = " 2024 ",
                ),
                InvalidTestCase(
                    testName = "non padded month",
                    value = "2024-1-01",
                ),
                InvalidTestCase(
                    testName = "non padded day",
                    value = "2024-01-1",
                ),
                InvalidTestCase(
                    testName = "truncated time",
                    value = "2024-01-01T15",
                ),
                InvalidTestCase(
                    testName = "space for date/time separator",
                    value = "2024-01-01 08:00:00",
                ),
                InvalidTestCase(
                    testName = "non padded hour",
                    value = "2024-01-01T8:00:00",
                ),
                InvalidTestCase(
                    testName = "non padded minute",
                    value = "2024-01-01T08:0:00",
                ),
                InvalidTestCase(
                    testName = "non padded second",
                    value = "2024-01-01T08:00:0",
                ),
                InvalidTestCase(
                    testName = "non padded hour offset",
                    value = "2024-01-01T08:00:00+1:00",
                ),
                InvalidTestCase(
                    testName = "non padded minute offset",
                    value = "2024-01-01T08:00:00+01:0",
                ),
                InvalidTestCase(
                    testName = "non padded second offset",
                    value = "2024-01-01T08:00:00+01:00:0",
                ),
                InvalidTestCase(
                    testName = "hour/min offset with no colon",
                    value = "2024-01-01T08:00:00+0100",
                ),
                InvalidTestCase(
                    testName = "hour/min/sec offset with no colon",
                    value = "2024-01-01T08:00:00+010000",
                ),
                InvalidTestCase(
                    testName = "hour only offset",
                    value = "2024-01-01T10:00:00+00",
                ),
                InvalidTestCase(
                    testName = "offset too big",
                    value = "2024-01-01T10:00:00+20:00",
                ),
                InvalidTestCase(
                    testName = "offset too small",
                    value = "2024-01-01T10:00:00-20:00",
                ),
                InvalidTestCase(
                    testName = "non existent timezone ID",
                    value = "2024-01-01T10:00:00[Atlantis/City]",
                ),
            ) { (value) ->
                shouldThrow<IllegalArgumentException> {
                    PartialDateTime.parse(value)
                }
            }
        }
    },
)
