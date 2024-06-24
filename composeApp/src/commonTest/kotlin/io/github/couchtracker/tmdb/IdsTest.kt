package io.github.couchtracker.tmdb

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import org.junit.jupiter.api.assertThrows

class IdsTest : FunSpec(
    {
        context("TmdbMovieId") {
            withData(0, -1) { id ->
                assertThrows<IllegalArgumentException> {
                    TmdbMovieId(id)
                }
            }
        }
        context("TmdbShowId") {
            withData(0, -1) { id ->
                assertThrows<IllegalArgumentException> {
                    TmdbShowId(id)
                }
            }
        }
    },
)
