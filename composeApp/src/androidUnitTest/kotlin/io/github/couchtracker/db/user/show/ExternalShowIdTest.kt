package io.github.couchtracker.db.user.show

import io.github.couchtracker.tmdb.TmdbShowId
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class ExternalShowIdTest : FunSpec(
    {
        context("parse()") {
            context("works") {
                withData(
                    "tmdb-1234" to TmdbExternalShowId(TmdbShowId(1234)),
                    "tmdb-9999" to TmdbExternalShowId(TmdbShowId(9999)),
                    "abcd-qwerty" to UnknownExternalShowId("abcd", "qwerty"),
                ) { (id, expected) ->
                    ExternalShowId.parse(id) shouldBe expected
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
                        ExternalShowId.parse(id)
                    }
                }
            }
        }
    },
)
