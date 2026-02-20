package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.kotest.core.spec.style.FunSpec

class ExternalEpisodeIdTest : FunSpec(
    {
        testParseAndSerialize(
            type = ExternalEpisodeId,
            validTestCases = listOf(
                ExternalIdParseSerializeTest(
                    id = TmdbExternalEpisodeId(TmdbEpisodeId(showId = 1234, seasonNumber = 1, episodeNumber = 5)),
                    values = listOf("tmdb-1234-s1e5", "tmdb-01234-s01e05", "tmdb-1234-s1e00005", "tmdb-000001234-s000001e00005"),
                ),
                ExternalIdParseSerializeTest(
                    id = TmdbExternalEpisodeId(TmdbEpisodeId(showId = 9999, seasonNumber = 0, episodeNumber = 3)),
                    values = listOf("tmdb-9999-s0e3"),
                ),
                ExternalIdParseSerializeTest(
                    id = TmdbExternalEpisodeId(TmdbEpisodeId(showId = 1, seasonNumber = 1, episodeNumber = 1)),
                    values = listOf("tmdb-1-s1e1"),
                ),
                ExternalIdParseSerializeTest(
                    id = UnknownExternalEpisodeId("abcd", "qwerty"),
                    values = listOf("abcd-qwerty"),
                ),
            ),
            invalidValues = listOf(
                "tmdb-1234-S1E5",
                "tmdb-abcd",
                "aaaaaaaaa",
                "   ",
                "",
            ),
        )
    },
)
