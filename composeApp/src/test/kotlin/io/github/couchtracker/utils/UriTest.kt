package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class UriTest : FunSpec(
    {
        context("encodeUriQuery()") {
            withData(
                "" to "",
                " " to "%20",
                "hello" to "hello",
                "hello world" to "hello%20world",
                "hello+world" to "hello+world",
                "hello/world" to "hello/world",
                "hello#world" to "hello%23world",
            ) { (decoded, expected) ->
                encodeUriQuery(decoded) shouldBe expected
            }
        }
    },
)
