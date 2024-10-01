package io.github.couchtracker.db.user.model.text

import io.github.couchtracker.db.user.CouchTrackerUri
import io.github.couchtracker.utils.parseUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class DbTextTest : FunSpec(
    {
        context("parseUri()") {
            context("default text") {
                context("valid URIs return the correct text") {
                    withData(
                        "couch-tracker://text/default/place-home" to DbDefaultText.PLACE_HOME.toDbText(),
                        "couch-tracker://text/default/place-plane" to DbDefaultText.PLACE_PLANE.toDbText(),
                    ) { (uriString, expectedText) ->
                        val uri = CouchTrackerUri(parseUri(uriString))
                        DbText.fromUri(uri) shouldBe expectedText
                    }
                }
                context("invalid default text returns unknown text") {
                    withData(
                        "couch-tracker://text/default/i-dont-exist",
                        "couch-tracker://text/default/aaaaaaaaaaaaaaaaa",
                    ) { uriString ->
                        val uri = CouchTrackerUri(parseUri(uriString))
                        val id = uriString.split('/').last()
                        DbText.fromUri(uri) shouldBe DbText.UnknownDefault(id, uri)
                    }
                }
                context("invalid default URIs fail") {
                    withData(
                        "couch-tracker://text/default",
                        "couch-tracker://text/default/",
                        "couch-tracker://text/default/%20",
                    ) { uriString ->
                        val uri = CouchTrackerUri(parseUri(uriString))
                        shouldThrow<IllegalArgumentException> {
                            DbText.fromUri(uri)
                        }
                    }
                }
            }
            context("custom text") {
                withData(
                    "couch-tracker://text/custom?hello" to "hello",
                    "couch-tracker://text/custom?" to "",
                    "couch-tracker://text/custom?hello%20world" to "hello world",
                    "couch-tracker://text/custom?special+chars-in!text/wow" to "special+chars-in!text/wow",
                    "couch-tracker://text/custom?special%2Bchars-in%21text%2Fwow" to "special+chars-in!text/wow",
                ) { (uriString, expectedText) ->
                    val uri = CouchTrackerUri(parseUri(uriString))
                    DbText.fromUri(uri) shouldBe DbText.Custom(expectedText)
                }
            }
            context("invalid URIs fail") {
                withData(
                    "couch-tracker://text",
                    "couch-tracker://text/",
                    "couch-tracker://text/new-format/abc?xyz=123",

                    // Wrong authority
                    "couch-tracker://icon/home",
                    "couch-tracker://icon/default/home",
                ) { uriString ->
                    val uri = CouchTrackerUri(parseUri(uriString))
                    shouldThrow<IllegalArgumentException> {
                        DbText.fromUri(uri)
                    }
                }
            }
        }
    },
)
