package io.github.couchtracker.tmdb

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class TmdbRatingTest : FunSpec(
    {
        context("ofOrNull") {
            data class TestCase(
                val avg: Float?,
                val count: Int?,
                val expectedAvg: Float?,
                val expectedCount: Int?,
            )
            withData(
                TestCase(avg = 3f, count = 2, expectedAvg = 3f, expectedCount = 2),
                TestCase(avg = 3f, count = null, expectedAvg = 3f, expectedCount = null),
                TestCase(avg = 0f, count = null, expectedAvg = null, expectedCount = null),
                TestCase(avg = -3f, count = 10, expectedAvg = null, expectedCount = null),
                TestCase(avg = 3f, count = 0, expectedAvg = null, expectedCount = null),
                TestCase(avg = null, count = 0, expectedAvg = null, expectedCount = null),
                TestCase(avg = null, count = 10, expectedAvg = null, expectedCount = null),
                TestCase(avg = null, count = null, expectedAvg = null, expectedCount = null),
            ) { testCase ->
                val parsed = TmdbRating.ofOrNull(testCase.avg, testCase.count)
                if (testCase.expectedAvg == null) {
                    parsed.shouldBeNull()
                } else {
                    parsed.shouldNotBeNull()
                    parsed.average shouldBe testCase.expectedAvg
                    parsed.count shouldBe testCase.expectedCount
                }
            }
        }
    },
)
