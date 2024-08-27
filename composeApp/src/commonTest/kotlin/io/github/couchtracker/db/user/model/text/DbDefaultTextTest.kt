package io.github.couchtracker.db.user.model.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class DbDefaultTextTest : FunSpec(
    {
        context("id") {
            withData(
                DbDefaultText.HOME to "home",
                DbDefaultText.PLANE to "plane",
            ) { (text, expectedId) ->
                text.id shouldBe expectedId
            }
        }
    },
)
