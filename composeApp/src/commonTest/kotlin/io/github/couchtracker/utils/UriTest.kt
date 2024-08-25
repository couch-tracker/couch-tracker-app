package io.github.couchtracker.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class UriTest : FunSpec(
    {
        // This class does some very basic testing, but assumes the implementors of the Uri interface will use a well-tested library/API

        context("valid URIs") {
            withData(
                nameFn = { it.ifEmpty { "<empty>" } },
                "",
                "https://example.com",
                "couch-tracker://abc/xyz",
                "mailto:hello@example.com",
                "urn:isbn:096139210x",
            ) {
                val uri = parseUri(it)
                uri.serialize() shouldBe it
            }
        }

        context("invalid URIs") {
            withData(
                nameFn = { it.ifBlank { "<blank>" } },
                "I AM NOT AN URI",
                "  ",
                "no-scheme-specific-part:",
                "no-authority://",
            ) {
                shouldThrow<UriParseException> {
                    parseUri(it)
                }
            }
        }
    },
)
