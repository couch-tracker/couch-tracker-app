package io.github.couchtracker.ui

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll

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
                        ItemPosition(index, size)
                    }
                }
            }

            context("first & isInTopStartCorner") {
                withTests(
                    tuple(0, 5, true),
                    tuple(1, 5, false),
                    tuple(4, 5, false),
                    tuple(0, 1, true),
                ) { (index, size, expected) ->
                    checkAll(Arb.int(min = 1)) { columns ->
                        val position = ItemPosition(index, size, columns)
                        position.first shouldBe expected
                        position.isInTopStartCorner() shouldBe expected
                    }
                }
            }
            context("last") {
                withTests(
                    tuple(0, 5, false),
                    tuple(1, 5, false),
                    tuple(4, 5, true),
                    tuple(0, 1, true),
                ) { (index, size, expected) ->
                    checkAll(Arb.int(min = 1)) { columns ->
                        val position = ItemPosition(index, size, columns)
                        position.last shouldBe expected
                    }
                }
            }
            context("isInTopEndCorner") {
                withTests(
                    tuple(ItemPosition(0, 5, 10), false),
                    tuple(ItemPosition(1, 5, 10), false),
                    tuple(ItemPosition(4, 5, 10), true),
                    tuple(ItemPosition(4, 50, 10), false),
                    tuple(ItemPosition(9, 50, 10), true),
                    tuple(ItemPosition(19, 50, 10), false),
                    tuple(ItemPosition(0, 1, 1), true),
                    tuple(ItemPosition(0, 1, 10), true),
                ) { (position, expected) ->
                    position.isInTopEndCorner() shouldBe expected
                }
            }
            context("isInBottomStartCorner") {
                withTests(
                    tuple(ItemPosition(0, 5, 10), true),
                    tuple(ItemPosition(1, 5, 10), false),
                    tuple(ItemPosition(4, 5, 10), false),
                    tuple(ItemPosition(4, 50, 10), false),
                    tuple(ItemPosition(9, 50, 10), false),
                    tuple(ItemPosition(10, 50, 10), false),
                    tuple(ItemPosition(40, 50, 10), true),
                    tuple(ItemPosition(0, 1, 1), true),
                    tuple(ItemPosition(0, 1, 10), true),
                ) { (position, expected) ->
                    position.isInBottomStartCorner() shouldBe expected
                }
            }
            context("isInBottomEndCorner") {
                withTests(
                    tuple(ItemPosition(0, 5, 10), false),
                    tuple(ItemPosition(1, 5, 10), false),
                    tuple(ItemPosition(4, 5, 10), true),
                    tuple(ItemPosition(4, 50, 10), false),
                    tuple(ItemPosition(9, 50, 10), false),
                    tuple(ItemPosition(10, 50, 10), false),
                    tuple(ItemPosition(40, 50, 10), false),
                    tuple(ItemPosition(49, 50, 10), true),
                    tuple(ItemPosition(48, 49, 10), true),
                    tuple(ItemPosition(39, 50, 10), false),
                    tuple(ItemPosition(39, 49, 10), true),
                    tuple(ItemPosition(0, 1, 1), true),
                    tuple(ItemPosition(0, 1, 10), true),
                ) { (position, expected) ->
                    position.isInBottomEndCorner() shouldBe expected
                }
            }
        }
    },
)
