package io.github.couchtracker.ui

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

class ListItemPositionTest : FunSpec(
    {
        context("ListItemPosition") {
            context("constructor fails for invalid values") {
                withTests(
                    tuple(0, 0),
                    tuple(5, 5),
                    tuple(10, 5),
                    tuple(-5, 4),
                    tuple(-5, 10),
                    tuple(0, -5),
                    tuple(-2, -5),
                    tuple(-5, -2),
                    tuple(5, -2),
                ) { (index, size) ->
                    shouldThrow<IllegalArgumentException> {
                        ListItemPosition(index, size)
                    }
                }
            }

            context("first") {
                withTests(
                    tuple(ListItemPosition(0, 5), true),
                    tuple(ListItemPosition(1, 5), false),
                    tuple(ListItemPosition(4, 5), false),
                    tuple(ListItemPosition(0, 1), true),
                ) { (position, expected) ->
                    position.first shouldBe expected
                }
            }
            context("last") {
                withTests(
                    tuple(ListItemPosition(0, 5), false),
                    tuple(ListItemPosition(1, 5), false),
                    tuple(ListItemPosition(4, 5), true),
                    tuple(ListItemPosition(0, 1), true),
                ) { (position, expected) ->
                    position.last shouldBe expected
                }
            }
        }
    },
)
