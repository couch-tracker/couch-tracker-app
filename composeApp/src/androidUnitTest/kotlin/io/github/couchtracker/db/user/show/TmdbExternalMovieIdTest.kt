package io.github.couchtracker.db.user.show

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

class TmdbExternalShowIdTest : FunSpec(
    {
        test("fails with invalid ID") {
            assertThrows<IllegalArgumentException> {
                TmdbExternalShowId(-1)
            }
        }

        context("ofValue()") {
            context("works with valid values") {
                withData(
                    "1234" to TmdbExternalShowId(1234),
                    "1234" to TmdbExternalShowId(1234),
                ) { (value, expected) ->
                    TmdbExternalShowId.ofValue(value) shouldBe expected
                }
            }

            context("fails with invalid values") {
                withData(
                    "not an integer",
                    "0",
                    "-123",
                    "   123",
                    "123   ",
                ) { value ->
                    assertThrows<IllegalArgumentException> {
                        TmdbExternalShowId.ofValue(value)
                    }
                }
            }
        }
    },
)
