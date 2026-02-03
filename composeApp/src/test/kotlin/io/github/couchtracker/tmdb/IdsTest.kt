package io.github.couchtracker.tmdb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData

class IdsTest : FunSpec(
    {
        context("TmdbMovieId") {
            withData(0, -1) { id ->
                shouldThrow<IllegalArgumentException> {
                    TmdbMovieId(id)
                }
            }
        }
        context("TmdbShowId") {
            withData(0, -1) { id ->
                shouldThrow<IllegalArgumentException> {
                    TmdbShowId(id)
                }
            }
        }
        context("TmdbSeasonId") {
            withData(-1, -2, Int.MIN_VALUE) { number ->
                shouldThrow<IllegalArgumentException> {
                    TmdbSeasonId(showId = TmdbShowId(1234), number = number)
                }
            }
        }
        context("TmdbEpisodeId") {
            withData(0, -1, -2, Int.MIN_VALUE) { number ->
                shouldThrow<IllegalArgumentException> {
                    TmdbEpisodeId(seasonId = TmdbSeasonId(showId = TmdbShowId(1234), 1), number = number)
                }
            }
        }
    },
)
