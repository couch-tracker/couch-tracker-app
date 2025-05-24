package io.github.couchtracker.utils

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import java.net.URI

class UriTest : FunSpec(
    {
        context("encode/decode URI component") {
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
                ) { (decoded, encoded) ->
                    withClue("encode") {
                        encodeUriComponent(decoded) shouldBe encoded
                    }
                    withClue("decode") {
                        decodeUriComponent(encoded) shouldBe decoded
                    }
                }
            }
            test("escaped value is correctly parsed by URL") {
                checkAll(Arb.string()) { query ->
                    val encoded = encodeUriComponent(query)

                    URI("https://ciao.com?$encoded").query shouldBe query
                }
            }

            test("round trip") {
                checkAll(Arb.string()) {
                    decodeUriComponent(encodeUriComponent(it)) shouldBe it
                }
            }
        }

        context("pathSegments") {
            context("basic tests") {
                withData(
                    nameFn = { "${it.first} -> ${it.second}" },
                    "schema://authority" to emptyList(),
                    "schema://authority/" to emptyList(),
                    "schema://authority/hello/world" to listOf("hello", "world"),
                    "schema://authority/hello//world" to listOf("hello", "world"),
                    "schema://authority/hello/world?query#fragment" to listOf("hello", "world"),
                    "schema://authority/hello%2Fworld" to listOf("hello/world"),
                    "schema://authority/hello+world" to listOf("hello+world"),
                    "schema://authority/hello/world?key=val/ue" to listOf("hello", "world"),
                ) { (uri, segments) ->
                    URI(uri).pathSegments() shouldBe segments
                }
            }

            test("round trip") {
                checkAll(Arb.list(Arb.string(minSize = 1))) { paths ->
                    val uri = URI("schema://authority/" + paths.joinToString(separator = "/") { encodeUriComponent(it) })
                    uri.pathSegments() shouldBe paths
                }
            }
        }
    },
)
