package io.github.couchtracker.model.sort

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

class SortAndGroupByTest : FunSpec(
    {
        fun category(name: String) = TestSortCategory(name)

        context("sortAndGroupBy") {
            test("returns empty when there are no items") {
                val result = emptyList<String>().sortAndGroupBy(
                    itemCategories = { listOf(SortCategory.Unsupported) },
                    comparator = naturalOrder(),
                )
                result shouldBe emptyList()
            }

            context("when items have no category") {
                test("puts all unsupported items in one group") {
                    val items = listOf("a", "b", "c")

                    val result = items.sortAndGroupBy(
                        itemCategories = { listOf(SortCategory.Unsupported) },
                        comparator = naturalOrder(),
                    )

                    result shouldHaveSize 1
                    result.single().first shouldBe SortCategory.Unsupported
                    result.single().second shouldContainExactly items
                }
            }

            context("when all items have the same category") {
                test("uses the most specific group") {
                    val a = category("A")
                    val items = listOf("1", "2", "3")

                    val result = items.sortAndGroupBy(
                        itemCategories = { listOf(SortCategory.RootCategory, a) },
                        comparator = naturalOrder(),
                    )

                    result shouldHaveSize 1
                    result.single().first shouldBe a
                    result.single().second shouldContainExactly items
                }
            }

            context("when there are multiple categories") {
                test("does not split when the largest category has fewer than 3 items") {
                    val a = category("A")
                    val b = category("B")
                    val items = listOf("a1", "a2", "b1")

                    val result = items.sortAndGroupBy(
                        itemCategories = {
                            when {
                                it.startsWith("a") -> listOf(SortCategory.RootCategory, a)
                                else -> listOf(SortCategory.RootCategory, b)
                            }
                        },
                        comparator = naturalOrder(),
                    )

                    result shouldHaveSize 1
                    result.single().second shouldContainExactly items
                }

                test("does not split when the median category has fewer than 2 items") {
                    val a = category("A")
                    val b = category("B")
                    val items = listOf("a1", "a2", "a3", "b1")

                    val result = items.sortAndGroupBy(
                        itemCategories = {
                            when {
                                it.startsWith("a") -> listOf(SortCategory.RootCategory, a)
                                else -> listOf(SortCategory.RootCategory, b)
                            }
                        },
                        comparator = naturalOrder(),
                    )

                    result shouldHaveSize 1
                    result.single().second shouldContainExactly items
                }

                test("splits when both categories meet the minimum sizes") {
                    val a = category("A")
                    val b = category("B")
                    val items = listOf("a1", "a2", "a3", "b1", "b2")

                    val result = items.sortAndGroupBy(
                        itemCategories = {
                            when {
                                it.startsWith("a") -> listOf(SortCategory.RootCategory, a)
                                else -> listOf(SortCategory.RootCategory, b)
                            }
                        },
                        comparator = naturalOrder(),
                    )

                    result shouldHaveSize 2
                    result.first { it.first == a }.second shouldContainExactly listOf("a1", "a2", "a3")
                    result.first { it.first == b }.second shouldContainExactly listOf("b1", "b2")
                }
            }

            context("when categories are nested") {

                test("recursively splits into subcategories") {
                    val a = category("A")
                    val b = category("B")
                    val a1 = category("A1")
                    val a2 = category("A2")

                    val items = listOf(
                        "a1-1",
                        "a1-2",
                        "a1-3",
                        "a2-1",
                        "a2-2",
                        "a2-3",
                        "b-1",
                        "b-2",
                        "b-3",
                    )

                    val result = items.sortAndGroupBy(
                        itemCategories = {
                            when {
                                it.startsWith("a1") -> listOf(SortCategory.RootCategory, a, a1)
                                it.startsWith("a2") -> listOf(SortCategory.RootCategory, a, a2)
                                else -> listOf(SortCategory.RootCategory, b)
                            }
                        },
                        comparator = naturalOrder(),
                    )

                    result.map { it.first } shouldContainExactly listOf(a1, a2, b)
                    result.first { it.first == a1 }.second shouldContainExactly listOf("a1-1", "a1-2", "a1-3")
                    result.first { it.first == a2 }.second shouldContainExactly listOf("a2-1", "a2-2", "a2-3")
                    result.first { it.first == b }.second shouldContainExactly listOf("b-1", "b-2", "b-3")
                }

                test("does not split a category when its items do not all have a deeper category") {
                    val a = category("A")
                    val b = category("B")
                    val a1 = category("A1")

                    val items = listOf(
                        "a1-1",
                        "a1-2",
                        "a1-3",
                        "a-without-child-1",
                        "a-without-child-2",
                        "a-without-child-3",
                        "b-1",
                        "b-2",
                        "b-3",
                    )

                    val result = items.sortAndGroupBy(
                        itemCategories = {
                            when {
                                it.startsWith("a1") -> listOf(SortCategory.RootCategory, a, a1)
                                it.startsWith("a-without") -> listOf(SortCategory.RootCategory, a)
                                else -> listOf(SortCategory.RootCategory, b)
                            }
                        },
                        comparator = naturalOrder(),
                    )

                    result.map { it.first } shouldContainExactly listOf(a, b)

                    result.first { it.first == a }.second shouldContainExactly listOf(
                        "a-without-child-1",
                        "a-without-child-2",
                        "a-without-child-3",
                        "a1-1",
                        "a1-2",
                        "a1-3",
                    )
                }
            }

            test("preserves every item exactly once") {
                val a = category("A")
                val b = category("B")

                val items = listOf(
                    "a1",
                    "a2",
                    "a3",
                    "b1",
                    "b2",
                    "b3",
                )

                val result = items.sortAndGroupBy(
                    itemCategories = {
                        when {
                            it.startsWith("a") -> listOf(SortCategory.RootCategory, a)
                            it.startsWith("b") -> listOf(SortCategory.RootCategory, b)
                            else -> error("Unexpected item")
                        }
                    },
                    comparator = naturalOrder(),
                )

                result.flatMap { it.second } shouldContainExactly items
            }
        }
    },
)

private data class TestSortCategory(
    val name: String,
) : SortCategory {
    override fun localize(context: Context) = throw UnsupportedOperationException()
}
