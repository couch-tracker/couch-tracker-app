package io.github.couchtracker

import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withContexts
import io.kotest.datatest.withTests
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

object SortTestUtils {

    fun <T> runComparingTest(items: List<T>, comparator: Comparator<T>) {
        // Check that every item in the list has the expected compareTo() result with every other item in the list (including itself)
        for ((ia, a) in items.withIndex()) {
            for (b in items.subList(fromIndex = ia + 1, toIndex = items.size)) {
                comparator.compare(a, b) shouldBeLessThan 0
                comparator.compare(b, a) shouldBeGreaterThan 0
            }
            comparator.compare(a, a) shouldBe 0
        }
    }
}

suspend fun <T : Comparable<T>> FunSpecContainerScope.testComparables(name: String, vararg items: T) {
    testComparator(
        name = name,
        comparator = Comparator.comparing { it },
        items = items,
    )
}

suspend fun <T : Comparable<T>> FunSpecContainerScope.testComparator(name: String, comparator: Comparator<in T>, vararg items: T) {
    test(name) {
        SortTestUtils.runComparingTest(items.toList(), comparator)
    }
}

suspend fun <T> FunSpecContainerScope.testComparators(
    comparators: Map<String, Comparator<in T>>,
    testCases: Map<String, List<T>>,
) {
    withContexts(comparators) { comparator ->
        withTests(testCases) { items ->
            SortTestUtils.runComparingTest(items, comparator)
        }
    }
}
