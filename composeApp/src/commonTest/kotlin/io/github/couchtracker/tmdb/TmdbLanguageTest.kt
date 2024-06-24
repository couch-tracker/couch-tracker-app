package io.github.couchtracker.tmdb

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class TmdbLanguageTest : FunSpec(
    {
        context("parse()") {
            context("works") {
                withData(
                    "en-US" to TmdbLanguage("en", "US"),
                    "en" to TmdbLanguage("en", null),
                    "it-IT" to TmdbLanguage("it", "IT"),
                ) { (id, expected) ->
                    TmdbLanguage.parse(id) shouldBe expected
                }
            }
            context("symmetric") {
                withData(
                    "en-US",
                    "en",
                    "it-IT",
                ) { str ->
                    TmdbLanguage.parse(str).serialize() shouldBe str
                }
            }
            context("fails with invalid IDs") {
                withData(
                    nameFn = { it.ifBlank { "<blank>" } },
                    "EN",
                    "en-us",
                    "en-USA",
                    "en-US-US",
                    "--",
                    "asd",
                ) { id ->
                    shouldThrow<IllegalArgumentException> {
                        TmdbLanguage.parse(id)
                    }
                }
            }
        }
    },
)
