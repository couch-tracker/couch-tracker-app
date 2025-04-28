package io.github.couchtracker.db.profile

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import java.net.URI

class CouchTrackerUriTest : FunSpec(
    {
        context("constructor") {
            context("fails with invalid URI") {
                withData(
                    mapOf(
                        "no schema" to "/",
                        "wrong schema" to "https://example.com",
                        "no authority" to "couch-tracker:///",
                        "wrong authority" to "couch-tracker://wrong",
                    ),
                ) {
                    val uri = URI(it)
                    shouldThrow<IllegalArgumentException> {
                        CouchTrackerUri(uri)
                    }
                }
            }
            context("works with valid URI") {
                class TestCase(
                    uri: String,
                    val expectedAuthority: CouchTrackerUri.Authority,
                ) {
                    val uri = URI(uri)
                }
                withData(
                    nameFn = { it.uri.toString() },
                    TestCase(
                        uri = "couch-tracker://icon",
                        expectedAuthority = CouchTrackerUri.Authority.ICON,
                    ),
                    TestCase(
                        uri = "couch-tracker://icon/",
                        expectedAuthority = CouchTrackerUri.Authority.ICON,
                    ),
                    TestCase(
                        uri = "couch-tracker://icon/xyz",
                        expectedAuthority = CouchTrackerUri.Authority.ICON,
                    ),
                ) { tc ->
                    val ctUri = shouldNotThrowAny {
                        CouchTrackerUri(tc.uri)
                    }

                    withClue("authority returns expected value") {
                        ctUri.authority shouldBe tc.expectedAuthority
                    }
                }
            }
        }
    },
)
