package io.github.couchtracker.db.profile.externalids

import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.core.spec.style.FunSpec

class ExternalShowIdTest : FunSpec(
    {
        testParseAndSerialize(
            type = ExternalShowId,
            validTestCases = listOf(
                ExternalIdParseSerializeTest(
                    id = TmdbExternalShowId(TmdbShowId(1234)),
                    values = listOf("tmdb-1234", "tmdb-01234", "tmdb-00000001234"),
                ),
                ExternalIdParseSerializeTest(
                    id = TmdbExternalShowId(TmdbShowId(999_999)),
                    values = listOf("tmdb-999999"),
                ),
                ExternalIdParseSerializeTest(
                    id = TmdbExternalShowId(TmdbShowId(1)),
                    values = listOf("tmdb-1"),
                ),
                ExternalIdParseSerializeTest(
                    id = UnknownExternalShowId("abcd", "qwerty"),
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
