package io.github.couchtracker.db.profile.model.text

import io.github.couchtracker.db.profile.CouchTrackerUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.enum
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.merge
import io.kotest.property.arbitrary.string
import io.kotest.property.forAll
import java.net.URI

class DbTextTest : FunSpec(
    {
        context("default text or unknown") {
            uriTests(
                valid = listOf(
                    "couch-tracker://text/default/place-home" to DbDefaultText.PLACE_HOME.toDbText(),
                    "couch-tracker://text/default/place-plane" to DbDefaultText.PLACE_PLANE.toDbText(),
                    "couch-tracker://text/default/i-dont-exist" to DbText.UnknownDefault("i-dont-exist"),
                    "couch-tracker://text/default/aaaaaaaaaaaaaaaaa" to DbText.UnknownDefault("aaaaaaaaaaaaaaaaa"),
                ),
                invalid = listOf(
                    "couch-tracker://text/default",
                    "couch-tracker://text/default/",
                    "couch-tracker://text/default/%20",
                    "couch-tracker://text/default/i-dont-exist#some-fragment",
                    "couch-tracker://text/default/place-home#some-fragment",
                ),
            )
        }
        context("custom text") {
            uriTests(
                valid = listOf(
                    "couch-tracker://text/custom?hello" to DbText.Custom("hello"),
                    "couch-tracker://text/custom?" to DbText.Custom(""),
                    "couch-tracker://text/custom?hello%20world" to DbText.Custom("hello world"),
                    "couch-tracker://text/custom?special%2Bchars-in%21text%2Fwow" to DbText.Custom("special+chars-in!text/wow"),
                ),
                invalid = listOf(
                    "couch-tracker://text/custom/something-else",
                    "couch-tracker://text/custom?hello#some-fragment",
                ),
            )
        }
        test("round trip") {
            forAll(Arb.defaultDbText()) {
                it == DbText.fromUri(it.toCouchTrackerUri())
            }
        }
        context("other invalid URIs fail") {
            withData(
                "couch-tracker://text",
                "couch-tracker://text/",
                "couch-tracker://text/new-format/abc?xyz=123",

                // Wrong authority
                "couch-tracker://icon/home",
                "couch-tracker://icon/default/home",
            ) { uriString ->
                val uri = CouchTrackerUri(URI(uriString))
                shouldThrow<IllegalArgumentException> {
                    DbText.fromUri(uri)
                }
            }
        }
    },
)

private suspend fun FunSpecContainerScope.uriTests(
    valid: List<Pair<String, DbText>>,
    invalid: List<String>,
) {
    val uriToDbText = valid
        .map { (uri, dbText) ->
            CouchTrackerUri(URI(uri)) to dbText
        }

    context("toCouchTrackerUri()") {
        withData(uriToDbText) { (ctUri, dbText) ->
            dbText.toCouchTrackerUri() shouldBe ctUri
        }
    }
    context("fromUri()") {
        context("valid URIs return the correct text") {
            withData(uriToDbText) { (ctUri, dbText) ->
                DbText.fromUri(ctUri) shouldBe dbText
            }
        }
        context("invalid URIs fail") {
            withData(invalid) { uri ->
                val ctUri = CouchTrackerUri(URI(uri))
                shouldThrow<IllegalArgumentException> {
                    DbText.fromUri(ctUri)
                }
            }
        }
    }
}

private fun Arb.Companion.defaultDbText(): Arb<DbText> {
    val default = enum<DbDefaultText>().map { DbText.Default(it) }
    val unknown = string(minSize = 1).map { DbText.UnknownDefault(it) }
    val custom = string().map { DbText.Custom(it) }

    return default.merge(unknown).merge(custom)
}
