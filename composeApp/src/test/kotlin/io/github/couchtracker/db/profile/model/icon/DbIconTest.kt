package io.github.couchtracker.db.profile.model.icon

import io.github.couchtracker.db.profile.CouchTrackerUri
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import java.net.URI

class DbIconTest : FunSpec(
    {
        context("parseUri()") {
            context("default icon") {
                context("valid URIs return the correct icon") {
                    withData(
                        "couch-tracker://icon/default/house" to DbDefaultIcon.HOUSE.toDbIcon(),
                        "couch-tracker://icon/default/cinema" to DbDefaultIcon.CINEMA.toDbIcon(),
                        "couch-tracker://icon/default/office-building" to DbDefaultIcon.OFFICE_BUILDING.toDbIcon(),
                    ) { (uriString, expectedIcon) ->
                        val uri = CouchTrackerUri(URI(uriString))
                        DbIcon.fromUri(uri) shouldBe expectedIcon
                    }
                }
                context("invalid default icon returns unknown icon") {
                    withData(
                        "couch-tracker://icon/default/i-dont-exist",
                        "couch-tracker://icon/default/aaaaaaaaaaaaaaaaa",
                    ) { uriString ->
                        val uri = CouchTrackerUri(URI(uriString))
                        DbIcon.fromUri(uri) shouldBe DbIcon.UnknownDefault(uri)
                    }
                }
                context("invalid default URIs fail") {
                    withData(
                        "couch-tracker://icon/default",
                        "couch-tracker://icon/default/",
                        "couch-tracker://icon/default/%20",
                    ) { uriString ->
                        val uri = CouchTrackerUri(URI(uriString))
                        shouldThrow<IllegalArgumentException> {
                            DbIcon.fromUri(uri)
                        }
                    }
                }
            }
            context("unknown icon URI format returns unknown icon") {
                withData(
                    "couch-tracker://icon",
                    "couch-tracker://icon/",
                    "couch-tracker://icon/new-format/abc?xyz=123",
                ) { uriString ->
                    val uri = CouchTrackerUri(URI(uriString))
                    DbIcon.fromUri(uri) shouldBe DbIcon.UnknownDefault(uri)
                }
            }
            context("URI with wrong authority fails") {
                withData(
                    "couch-tracker://text/home",
                    "couch-tracker://text/default/home",
                ) { uriString ->
                    val uri = CouchTrackerUri(URI(uriString))
                    shouldThrow<IllegalArgumentException> {
                        DbIcon.fromUri(uri) shouldBe DbIcon.UnknownDefault(uri)
                    }
                }
            }
        }
    },
)
