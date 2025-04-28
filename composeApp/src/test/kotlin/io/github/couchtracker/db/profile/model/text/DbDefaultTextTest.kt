package io.github.couchtracker.db.profile.model.text

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class DbDefaultTextTest : FunSpec(
    {
        context("id") {
            withData(
                DbDefaultText.PLACE_HOME to "place-home",
                DbDefaultText.PLACE_PLANE to "place-plane",
            ) { (text, expectedId) ->
                text.id shouldBe expectedId
            }
        }
    },
)
