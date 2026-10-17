package io.github.couchtracker.model.sort

import io.github.couchtracker.model.sort.SortCategory.TmdbRatingCategory
import io.github.couchtracker.tmdb.TmdbRating
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.comparables.shouldBeGreaterThanOrEqualTo
import io.kotest.matchers.floats.shouldBeLessThan
import io.kotest.matchers.ranges.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.float
import io.kotest.property.arbitrary.map
import io.kotest.property.checkAll
import kotlin.math.floor

private val sorter = Sorter.ByTmdbRating()

class RatingCategoryTest : FunSpec(
    {

        context("property categories") {
            test("always starts at the root") {
                checkAll(Arb.ratingCategory(max = 10f)) { (_, category) ->
                    category[0] shouldBe SortCategory.RootCategory
                }
            }

            test("ratings up to 5 belong to the lower group") {
                checkAll(Arb.ratingCategory(min = 0.0f, max = 5.999f)) { (_, category) ->
                    category[1] shouldBe TmdbRatingCategory(0..5)
                }
            }

            test("ratings from 6 belong to the upper group") {
                checkAll(Arb.ratingCategory(min = 6.0f, max = 10.0f)) { (_, category) ->
                    category[1] shouldBe TmdbRatingCategory(6..10)
                }
            }

            test("integer range starts at an even number and ends at the next odd number") {
                checkAll(Arb.ratingCategory()) { (_, category) ->
                    val range = category[2] as TmdbRatingCategory
                    range.stars.first shouldBe range.stars.last - 1
                    range.stars.first % 2 shouldBe 0
                }
            }

            test("exact integer category contains the truncated rating") {
                checkAll(Arb.ratingCategory()) { (rating, category) ->
                    category[3] shouldBe
                        TmdbRatingCategory(rating.average.toInt()..rating.average.toInt())
                }
            }

            test("decimal half range starts at the lower half-step") {
                checkAll(Arb.ratingCategory()) { (rating, category) ->
                    val start = floor(rating.average * 2) / 2
                    val range = category[4] as SortCategory.TmdbRatingCategoryDecimal
                    range.stars.start shouldBe start
                    range.stars.endInclusive shouldBe start + 0.4f
                }
            }
        }

        context("boundary examples") {
            test("decimal half range changes at .5") {
                ratingOf(7.499f).categories()[4] shouldBe
                    SortCategory.TmdbRatingCategoryDecimal(7.0f..7.4f)

                ratingOf(7.5f).categories()[4] shouldBe
                    SortCategory.TmdbRatingCategoryDecimal(7.5f..7.9f)
            }
        }

        test("all ranges are within 0..10 and each category is contained by its parent") {
            checkAll(Arb.ratingCategory()) { (rating, category) ->
                val groupRange = (category[1] as TmdbRatingCategory).stars
                val integerRange = (category[2] as TmdbRatingCategory).stars
                val exactRange = (category[3] as TmdbRatingCategory).stars
                val decimalRange = (category[4] as SortCategory.TmdbRatingCategoryDecimal).stars

                rating.average.toInt() shouldBeIn groupRange
                rating.average.toInt() shouldBeIn integerRange
                rating.average.toInt() shouldBeIn exactRange
                rating.average shouldBeGreaterThanOrEqualTo decimalRange.start
                rating.average shouldBeLessThan decimalRange.endInclusive + 0.1f

                groupRange.first shouldBeIn 0..10
                groupRange.last shouldBeIn 0..10

                integerRange.first shouldBeIn groupRange
                integerRange.last shouldBeIn groupRange

                exactRange.first shouldBeIn integerRange
                exactRange.last shouldBeIn integerRange

                val exactRangeFloat = exactRange.first.toFloat()..(exactRange.last + 1).toFloat()
                decimalRange.start shouldBeIn exactRangeFloat
                decimalRange.endInclusive shouldBeIn exactRangeFloat
            }
        }

        test("categorise for 10") {
            // 10 stars is kind of a special case, so it's easier to treat it specially
            ratingOf(10f).categories() shouldBe listOf(
                SortCategory.RootCategory,
                TmdbRatingCategory(6..10),
                TmdbRatingCategory(10..10),
            )
        }
    },
)

private fun ratingOf(avg: Float) = TmdbRating(average = avg, count = null, formatted = "")

private fun TmdbRating.categories() =
    sorter.propertyCategories(this)

private fun Arb.Companion.ratingCategory(min: Float = 0f, max: Float = 9.99999f): Arb<Pair<TmdbRating, List<SortCategory>>> {
    return float(min..max, includeNaNs = false)
        .map { avg ->
            val rating = ratingOf(avg)
            rating to rating.categories()
        }
}
