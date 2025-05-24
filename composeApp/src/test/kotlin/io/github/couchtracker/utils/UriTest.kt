package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI

class UriTest : FunSpec(
    {
        context("encodeUriComponent()") {
            context("basic tests") {
                withData(
                    "" to "",
                    " " to "%20",
                    "hello" to "hello",
                    "hello-to_everyone.in~the.world" to "hello-to_everyone.in~the.world",
                    "hello world" to "hello%20world",
                    "hello+world" to "hello%2Bworld",
                    "hello/world" to "hello%2Fworld",
                    "hello#world" to "hello%23world",
                    "hello=world" to "hello%3Dworld",
                    "hello&world" to "hello%26world",
                ) { (decoded, expected) ->
                    encodeUriComponent(decoded) shouldBe expected
                }
            }
            test("escaped value is correctly parsed by URL") {
                checkAll(Arb.string()) { query ->
                    val encoded = encodeUriComponent(query)

                    URI("https://ciao.com?$encoded").query shouldBe query
                }
            }
        }
    },
)
