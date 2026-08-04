package io.github.couchtracker.utils.error

import io.github.couchtracker.utils.Loadable
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class AggregateResultsTest : FunSpec(
    {

        val error1 = ApiError.Simulated()
        val error2 = ApiError.ItemNotFound(RuntimeException(), itemIdentifier = null)

        test("returns all values when every result is loaded successfully") {
            val results = listOf(
                Loadable.value("one"),
                Loadable.value("two"),
                Loadable.value("three"),
            )

            results.aggregateResults() shouldBe
                Loadable.value(listOf("one", "two", "three"))
        }

        test("returns an empty list when the collection is empty") {
            emptyList<CouchTrackerLoadable<String>>().aggregateResults() shouldBe Loadable.value(emptyList())
        }

        test("returns Loading when any result is loading and there are no errors") {
            val results = listOf(
                Loadable.value("one"),
                Loadable.Loading,
                Loadable.value("three"),
            )

            results.aggregateResults() shouldBe Loadable.Loading
        }

        test("returns Loading when all results are loading") {
            val results = listOf(Loadable.Loading, Loadable.Loading)

            results.aggregateResults<Int>() shouldBe Loadable.Loading
        }

        test("returns the single error when there is one error") {
            val results = listOf(
                Loadable.value("one"),
                Loadable.error(error1),
                Loadable.value("three"),
            )
            results.aggregateResults() shouldBe Loadable.error(error1)
        }

        test("aggregates multiple errors") {
            val results = listOf(
                Loadable.error(error1),
                Loadable.error(error2),
            )

            results.aggregateResults() shouldBe
                Loadable.error(listOf(error1, error2).aggregateError())
        }

        test("errors take precedence over Loading") {
            val results = listOf(
                Loadable.value("one"),
                Loadable.Loading,
                Loadable.error(error1),
                Loadable.Loading,
            )

            results.aggregateResults() shouldBe
                Loadable.error(error1)
        }

        test("errors take precedence over successfully loaded values") {
            val results = listOf(
                Loadable.value("one"),
                Loadable.error(error1),
                Loadable.value("three"),
            )

            results.aggregateResults() shouldBe
                Loadable.error(error1)
        }

        test("preserves the order of loaded values") {
            val results = listOf(
                Loadable.value("first"),
                Loadable.value("second"),
                Loadable.value("third"),
            )

            results.aggregateResults() shouldBe
                Loadable.value(listOf("first", "second", "third"))
        }
    },
)
