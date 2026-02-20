package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

class ExternalIdTest : FunSpec(
    {
        val validTypedTestCases = listOf(
            TmdbMovieId(1234).toExternalId() to "movie-tmdb-1234",
            TmdbShowId(1234).toExternalId() to "show-tmdb-1234",
            TmdbSeasonId(TmdbShowId(1234), 2).toExternalId() to "season-tmdb-1234-s2",
            TmdbEpisodeId(1234, 1, 3).toExternalId() to "episode-tmdb-1234-s1e3",
            UnknownExternalMovieId("abcd", "12345") to "movie-abcd-12345",
        )
        context("serialize") {
            withTests(nameFn = { it.second }, validTypedTestCases) { (externalId, serializedValue) ->
                externalId.serialize() shouldBe serializedValue
            }
        }
        context("parse") {
            context("works") {
                withTests(nameFn = { it.first.toString() }, validTypedTestCases) { (externalId, serializedValue) ->
                    ExternalId.parse<ExternalId>(serializedValue) shouldBe externalId
                }
                test("using typed argument") {
                    ExternalId.parse<BookmarkableExternalId>("movie-tmdb-1234") shouldBe TmdbMovieId(1234).toExternalId()
                }
            }

            context("fails") {
                withTests(
                    mapOf(
                        "invalid string" to "abcdef",
                        "invalid type" to "invalid-tmdb-123",
                        "wrong type casing" to "MOVIE-tmdb-1234",
                        "non-typed external id" to "tmdb-1234",
                    ),
                ) { value ->
                    shouldThrow<IllegalArgumentException> {
                        ExternalId.parse<ExternalId>(value)
                    }
                }
                test("valid typed external id, wrong type argument") {
                    shouldThrow<IllegalArgumentException> {
                        ExternalId.parse<BookmarkableExternalId>("episode-tmdb-123-1x3")
                    }
                }
            }
        }
    },
)
