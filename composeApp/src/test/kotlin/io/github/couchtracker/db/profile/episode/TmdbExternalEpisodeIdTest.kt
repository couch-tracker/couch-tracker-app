package io.github.couchtracker.db.profile.episode

import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class TmdbExternalEpisodeIdTest : FunSpec(
    {
        context("ofValue()") {
            context("works with valid values") {
                withData(
                    "1234" to TmdbExternalEpisodeId(TmdbEpisodeId(1234)),
                    "1234" to TmdbExternalEpisodeId(TmdbEpisodeId(1234)),
                ) { (value, expected) ->
                    TmdbExternalEpisodeId.ofValue(value) shouldBe expected
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
                        TmdbExternalEpisodeId.ofValue(value)
                    }
                }
            }
        }
    },
)
