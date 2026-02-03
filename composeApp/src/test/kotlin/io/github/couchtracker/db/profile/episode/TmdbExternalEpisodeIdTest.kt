package io.github.couchtracker.db.profile.episode

import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class TmdbExternalEpisodeIdTest : FunSpec(
    {
        context("ofValue()") {
            context("works with valid values") {
                withData(
                    "1234-0x1" to TmdbExternalEpisodeId(TmdbShowId(1234).season(0).episode(1)),
                    "1234-1x2" to TmdbExternalEpisodeId(TmdbShowId(1234).season(1).episode(2)),
                    "2222-55x440" to TmdbExternalEpisodeId(TmdbShowId(2222).season(55).episode(440)),
                ) { (value, expected) ->
                    TmdbExternalEpisodeId.ofValue(value) shouldBe expected
                }
            }

            context("fails with invalid values") {
                withData(
                    "1111-1x1x1",
                    "1234-5-6",
                    "1234",
                    "  1111-1x1",
                    "1111-1x1   ",
                    "1234--1x5",
                    "1234-1x-5",
                ) { value ->
                    shouldThrow<IllegalArgumentException> {
                        TmdbExternalEpisodeId.ofValue(value)
                    }
                }
            }
        }
    },
)
