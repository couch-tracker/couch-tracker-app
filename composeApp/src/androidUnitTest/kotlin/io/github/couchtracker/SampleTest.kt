package io.github.couchtracker

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class SampleTest : FunSpec(
    {
        context("some sums") {
            withData(
                Triple(1, 2, 3),
                Triple(5, 6, 11),
            ) { (a, b, c) ->
                a + b shouldBe c
            }
        }
    },
)
