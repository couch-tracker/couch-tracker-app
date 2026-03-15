package io.github.couchtracker.intl.datetime

import io.github.couchtracker.utils.TickingValue
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.kotlinInstant
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.zoneId
import io.kotest.property.checkAll
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

/**
 * Runs tests that validate that the computed [TickingValue.nextTick] is correctly predicting a change in formatting.
 *
 * Two similar but distinct fuzzy tests are performed:
 * - one using arbitrary [T] values from the given [arb]
 * - one generating [T] values by using [valueFromInstant] and passing an [Instant] close to the generated now (between -100 and +100 days)
 *
 * The second case guarantees that more realistic cases (where now and the value to format are close) are covered.
 *
 * Note: the [T] value returned by [valueFromInstant] doesn't need to be "perfect", as it's only used to generate an input to the test.
 * Technically, even returning a completely random value won't make the tests fail, but it _will_ defeat the purpose of the second case.
 */
suspend fun <T> FunSpecContainerScope.nextTickPredictsChangeTest(
    arb: Arb<T>,
    valueFromInstant: (Instant, TimeZone) -> T,
    format: (T, now: Instant, tz: TimeZone) -> TickingValue<String>,
) {
    context("correctly predicts date change") {
        // This first test just checks arbitrary values
        // However, these values will likely be very far apart, and wouldn't necessarily test all relevant cases
        test("arbitrary values") {
            checkAll(
                arb,
                Arb.kotlinInstant(),
                Arb.zoneId().map { it.toKotlinTimeZone() },
            ) { value, now, tz ->
                runNextTickPredictsChangeTest(value, now, tz, format)
            }
        }

        // This test instead computes the value from a small amount of time away from now, ensuring more realistic cases are also covered
        test("values close to now") {
            checkAll(
                Arb.duration(-100.days..100.days),
                Arb.kotlinInstant(),
                Arb.zoneId().map { it.toKotlinTimeZone() },
            ) { nowDiff, now, tz ->
                val value = valueFromInstant(now + nowDiff, tz)
                runNextTickPredictsChangeTest(value, now, tz, format)
            }
        }
    }
}

private fun <T> runNextTickPredictsChangeTest(
    value: T,
    now: Instant,
    tz: TimeZone,
    format: (T, now: Instant, tz: TimeZone) -> TickingValue<String>,
) {
    val formatted = format(value, now, tz)

    val nextTick = withClue("next tick should never be null") {
        formatted.nextTick.shouldNotBeNull()
    }

    withClue("format at 1 nanosecond before nextTick should yield same relative value") {
        format(value, now + nextTick - 1.nanoseconds, tz) should {
            it.value shouldBe formatted.value
            it.nextTick shouldBe 1.nanoseconds
        }
    }

    withClue("format at nextTick should yield different relative value") {
        format(value, now + nextTick, tz) should {
            it.value shouldNotBe formatted.value
        }
    }
}
