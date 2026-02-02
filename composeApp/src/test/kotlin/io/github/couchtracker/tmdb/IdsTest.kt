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
        context("TmdbEpisodeId") {
            withData(0, -1) { id ->
                shouldThrow<IllegalArgumentException> {
                    TmdbShowId(id)
                }
            }
        }
    },
)
