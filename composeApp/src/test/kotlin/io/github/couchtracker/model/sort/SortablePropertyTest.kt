package io.github.couchtracker.model.sort

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class SortablePropertyTest : FunSpec(
    {
        context("compareSortableProperties") {

            test("sorts asc") {
                val values = listOf(
                    SortableProperty.Known(3),
                    SortableProperty.Unsupported,
                    SortableProperty.Unknown,
                    SortableProperty.Unknown,
                    SortableProperty.Known(1),
                    SortableProperty.Known(2),
                    SortableProperty.Unsupported,
                    SortableProperty.Error,
                    SortableProperty.Error,
                )

                val sorted = values.sortedWith(
                    Comparator { a, b -> compareSortableProperties(true, a, b) },
                )

                sorted shouldBe listOf(
                    SortableProperty.Known(1),
                    SortableProperty.Known(2),
                    SortableProperty.Known(3),
                    SortableProperty.Unknown,
                    SortableProperty.Unknown,
                    SortableProperty.Error,
                    SortableProperty.Error,
                    SortableProperty.Unsupported,
                    SortableProperty.Unsupported,
                )
            }

            test("sorts desc") {
                val values = listOf(
                    SortableProperty.Known(3),
                    SortableProperty.Error,
                    SortableProperty.Unsupported,
                    SortableProperty.Unknown,
                    SortableProperty.Unknown,
                    SortableProperty.Error,
                    SortableProperty.Known(1),
                    SortableProperty.Known(2),
                    SortableProperty.Unsupported,
                )

                val sorted = values.sortedWith(
                    Comparator { a, b -> compareSortableProperties(false, a, b) },
                )

                sorted shouldBe listOf(
                    SortableProperty.Known(3),
                    SortableProperty.Known(2),
                    SortableProperty.Known(1),
                    SortableProperty.Unknown,
                    SortableProperty.Unknown,
                    SortableProperty.Error,
                    SortableProperty.Error,
                    SortableProperty.Unsupported,
                    SortableProperty.Unsupported,
                )
            }
        }
    },
)
