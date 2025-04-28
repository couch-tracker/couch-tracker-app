package io.github.couchtracker.db.profile.episode

import io.github.couchtracker.tmdb.TmdbEpisodeId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ExternalEpisodeIdTest : FunSpec(
    {
        context("parse()") {
            context("works") {
                withData(
                    "tmdb-1234" to TmdbExternalEpisodeId(TmdbEpisodeId(1234)),
                    "tmdb-9999" to TmdbExternalEpisodeId(TmdbEpisodeId(9999)),
                    "abcd-qwerty" to UnknownExternalEpisodeId("abcd", "qwerty"),
                ) { (id, expected) ->
                    ExternalEpisodeId.parse(id) shouldBe expected
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
                        ExternalEpisodeId.parse(id)
                    }
                }
            }
        }
    },
)
