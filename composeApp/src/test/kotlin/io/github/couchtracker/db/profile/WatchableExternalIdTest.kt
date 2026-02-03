package io.github.couchtracker.db.profile

import io.github.couchtracker.db.profile.episode.UnknownExternalEpisodeId
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
                Pair(
                    WatchableExternalId.Episode(TmdbEpisodeId(showId = 999, seasonNumber = 1, episodeNumber = 1).toExternalId()),
                    "episode:tmdb-999-1x1",
                ),
            ) { (id, expected) ->
                id.serialize() shouldBe expected
            }
        }

        context("parse()") {
            context("works with valid values") {
                withData(
                    "movie:tmdb-123" to WatchableExternalId.Movie(TmdbMovieId(123).toExternalId()),
                    Pair(
                        "episode:tmdb-999-1x2",
                        WatchableExternalId.Episode(TmdbEpisodeId(showId = 999, seasonNumber = 1, episodeNumber = 2).toExternalId()),
                    ),
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
