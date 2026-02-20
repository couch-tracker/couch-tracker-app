package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbSeasonId
import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.core.spec.style.FunSpec

class ExternalSeasonIdTest : FunSpec(
    {
        testParseAndSerialize(
            type = ExternalSeasonId,
            validTestCases = listOf(
                ExternalIdParseSerializeTest(
                    id = TmdbExternalSeasonId(TmdbSeasonId(showId = TmdbShowId(1234), number = 1)),
                    values = listOf("tmdb-1234-s1", "tmdb-1234-s01", "tmdb-001234-s000001"),
                ),
                ExternalIdParseSerializeTest(
                    id = TmdbExternalSeasonId(TmdbSeasonId(showId = TmdbShowId(99_999), number = 0)),
                    values = listOf("tmdb-99999-s0"),
                ),
                ExternalIdParseSerializeTest(
                    id = UnknownExternalSeasonId("abcd", "qwerty"),
                    values = listOf("abcd-qwerty"),
                ),
            ),
            invalidValues = listOf(
                "tmdb-abcd",
                "aaaaaaaaa",
                "   ",
                "",
            ),
        )
    },
)
