package io.github.couchtracker.db.user.model.icon

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class DbDefaultIconTest : FunSpec(
    {
        context("id") {
            withData(
                DbDefaultIcon.HOME to "home",
                DbDefaultIcon.OFFICE_BUILDING to "office-building",
                DbDefaultIcon.CINEMA to "cinema",
            ) { (icon, expectedId) ->
                icon.id shouldBe expectedId
            }
        }
    },
)
