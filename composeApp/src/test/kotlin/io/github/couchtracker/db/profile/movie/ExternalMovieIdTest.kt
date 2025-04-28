package io.github.couchtracker.db.profile.movie

import io.github.couchtracker.tmdb.TmdbMovieId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ExternalMovieIdTest : FunSpec(
    {
        context("parse()") {
            context("works") {
                withData(
                    "tmdb-1234" to TmdbExternalMovieId(TmdbMovieId(1234)),
                    "tmdb-9999" to TmdbExternalMovieId(TmdbMovieId(9999)),
                    "abcd-qwerty" to UnknownExternalMovieId("abcd", "qwerty"),
                ) { (id, expected) ->
                    ExternalMovieId.parse(id) shouldBe expected
                }
            }

            context("fails with invalid IDs") {
                withData(
                    nameFn = { it.ifBlank { "<blank>" } },
                    "tmdb-abcd",
                    "aaaaaaaaa",
                    "   ",
                ) { id ->
                    shouldThrow<IllegalArgumentException> {
                        ExternalMovieId.parse(id)
                    }
                }
            }
        }
    },
)
