package io.github.couchtracker.tmdb

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.orNull
import io.kotest.property.checkAll

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
                TestCase(avg = 0f, count = -1, expectedAvg = null, expectedCount = null),
                TestCase(avg = 0f, count = 0, expectedAvg = null, expectedCount = null),
                TestCase(avg = 0f, count = 10, expectedAvg = 0f, expectedCount = 10),
                TestCase(avg = -3f, count = 10, expectedAvg = null, expectedCount = null),
                TestCase(avg = 3f, count = 0, expectedAvg = 3f, expectedCount = null),
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
            test("fuzz") {
                checkAll(Arb.float().orNull(), Arb.int().orNull()) { avg, count ->
                    // Doesn't throw
                    val rating = TmdbRating.ofOrNull(avg, count)
                    if (rating?.average != null) {
                        rating.average shouldBe avg
                    }
                    if (rating?.count != null) {
                        rating.count shouldBe count
                    }
                    if (avg != null && avg > 0f) {
                        rating?.average shouldBe avg
                    }
                }
            }
        }
    },
)
