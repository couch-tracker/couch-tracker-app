package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.profile.externalids.BookmarkableExternalId
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.db.profile.externalids.ExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalMovieId
import io.github.couchtracker.db.profile.externalids.UnknownExternalShowId
import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.core.spec.style.FunSpec

class ExternalIdColumnAdapterTest : FunSpec(
    {
        context("with type") {
            context("generic external ID") {
                testColumnAdapter(
                    columnAdapter = ExternalId.columnAdapter<ExternalId>(),
                    valid = listOf(
                        ColumnAdapterTest(databaseValue = "movie-tmdb-123", decodedValue = TmdbMovieId(123).toExternalId()),
                        ColumnAdapterTest(databaseValue = "episode-tmdb-123-s4e5", decodedValue = TmdbEpisodeId(123, 4, 5).toExternalId()),
                        ColumnAdapterTest(databaseValue = "show-abcd-123", decodedValue = UnknownExternalShowId("abcd", "123")),
                    ),
                    invalid = listOf(
                        "tmdb-123",
                        "movie-tmdb-aaaa",
                        "abcdef",
                    ),
                )
            }
            context("narrow external ID") {
                testColumnAdapter(
                    columnAdapter = ExternalId.columnAdapter<BookmarkableExternalId>(),
                    valid = listOf(
                        ColumnAdapterTest(databaseValue = "movie-tmdb-9876", decodedValue = TmdbMovieId(9876).toExternalId()),
                        ColumnAdapterTest(databaseValue = "show-tmdb-456", decodedValue = TmdbShowId(456).toExternalId()),
                    ),
                    invalid = listOf(
                        "invalid",
                        "season-tmdb-1234-s1",
                        "episode-tmdb-1234-s1e3",
                    ),
                )
            }
        }

        context("predetermined type") {
            testColumnAdapter(
                columnAdapter = ExternalMovieId.columnAdapter(),
                valid = listOf(
                    ColumnAdapterTest(databaseValue = "tmdb-123", decodedValue = TmdbMovieId(123).toExternalId()),
                    ColumnAdapterTest(databaseValue = "xyz-abcd", decodedValue = UnknownExternalMovieId("xyz", "abcd")),
                    ColumnAdapterTest(databaseValue = "movie-tmdb-123", decodedValue = UnknownExternalMovieId("movie", "tmdb-123")),
                ),
                invalid = listOf(
                    "tmdb-1234-s1",
                    "abcdef",
                ),
            )
        }
    },
)
