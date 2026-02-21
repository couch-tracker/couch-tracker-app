package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class TmdbExternalShowIdTest : FunSpec(
    {
        context("ofValue()") {
            context("works with valid values") {
                withData(
                    "1234" to TmdbExternalShowId(TmdbShowId(1234)),
                    "1234" to TmdbExternalShowId(TmdbShowId(1234)),
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
                    shouldThrow<IllegalArgumentException> {
                        TmdbExternalShowId.ofValue(value)
                    }
                }
            }
        }
    },
)
