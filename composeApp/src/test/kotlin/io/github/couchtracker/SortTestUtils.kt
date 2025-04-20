package io.github.couchtracker

import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.comparables.shouldBeEqualComparingTo
import io.kotest.matchers.comparables.shouldBeGreaterThan
import io.kotest.matchers.comparables.shouldBeLessThan

object SortTestUtils {
    fun <T : Comparable<T>> runComparablesTest(items: Iterable<T>) {
        val list = items.toList()

        // Check that every item in the list has the expected compareTo() result with every other item in the list (including itself)
        for ((ia, a) in list.withIndex()) {
            for (b in list.subList(fromIndex = ia + 1, toIndex = list.size)) {
                a shouldBeLessThan b
                b shouldBeGreaterThan a
            }
            a shouldBeEqualComparingTo a
        }
    }
}

suspend fun <T : Comparable<T>> FunSpecContainerScope.testComparables(name: String, vararg items: T) {
    test(name) {
        SortTestUtils.runComparablesTest(items.toList())
    }
}
