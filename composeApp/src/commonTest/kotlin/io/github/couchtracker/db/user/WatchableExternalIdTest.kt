package io.github.couchtracker.db.user

import io.github.couchtracker.db.user.episode.UnknownExternalEpisodeId
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class WatchableExternalIdTest : FunSpec(
    {
        context("serialize()") {
            withData(
                WatchableExternalId.Movie(TmdbMovieId(123).toExternalId()) to "movie:tmdb-123",
                WatchableExternalId.Episode(TmdbEpisodeId(999).toExternalId()) to "episode:tmdb-999",
            ) { (id, expected) ->
                id.serialize() shouldBe expected
            }
        }

        context("parse()") {
            context("works with valid values") {
                withData(
                    "movie:tmdb-123" to WatchableExternalId.Movie(TmdbMovieId(123).toExternalId()),
                    "episode:tmdb-999" to WatchableExternalId.Episode(TmdbEpisodeId(999).toExternalId()),
                    "episode:abc-xyz" to WatchableExternalId.Episode(UnknownExternalEpisodeId("abc", "xyz")),
                ) { (serializedValue, expected) ->
                    WatchableExternalId.parse(serializedValue) shouldBe expected
                }
            }
            context("fails with invalid values") {
                withData(
                    nameFn = { it.ifBlank { "<blank>" } },
                    "  ",
                    "invalid:tmdb-123",
                    "movie:aaaaaaa",
                    "tmdb-123",
                ) { value ->
                    shouldThrow<IllegalArgumentException> {
                        WatchableExternalId.parse(value)
                    }
                }
            }
        }
    },
)
