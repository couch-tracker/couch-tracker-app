package io.github.couchtracker.db.user.movie

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.assertThrows

class TmdbExternalMovieIdTest : FunSpec(
    {
        test("fails with invalid ID") {
            assertThrows<IllegalArgumentException> {
                TmdbExternalMovieId(-1)
            }
        }

        context("ofValue()") {
            context("works with valid values") {
                withData(
                    "1234" to TmdbExternalMovieId(1234),
                    "1234" to TmdbExternalMovieId(1234),
                ) { (value, expected) ->
                    TmdbExternalMovieId.ofValue(value) shouldBe expected
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
                        TmdbExternalMovieId.ofValue(value)
                    }
                }
            }
        }
    },
)
