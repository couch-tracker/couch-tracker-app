package io.github.couchtracker.intl.datetime

import io.github.couchtracker.utils.MaybeZoned
import io.github.couchtracker.utils.TickingValue
import io.github.couchtracker.utils.Zoned
import io.github.couchtracker.utils.plus
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.KotlinInstantRange
import io.kotest.property.arbitrary.arbitrary
import io.kotest.property.arbitrary.choose
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.kotlinInstant
import io.kotest.property.arbitrary.localDateTime
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.next
import io.kotest.property.arbitrary.zoneId
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toKotlinLocalDateTime
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
    valueFromInstant: (Zoned<Instant>) -> T,
    format: (T, now: Zoned<Instant>) -> TickingValue<String>,
    nowRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
) {
    context("correctly predicts date change") {
        // This first test just checks arbitrary values
        // However, these values will likely be very far apart, and wouldn't necessarily test all relevant cases
        test("arbitrary values") {
            checkAll(
                arb,
                Arb.zonedInstant(nowRange),
            ) { value, now ->
                formatAndTestNextTick(value, now, format)
            }
        }

        // This test instead computes the value from a small amount of time away from now, ensuring more realistic cases are also covered
        test("values close to now") {
            checkAll(
                Arb.duration(-100.days..100.days),
                Arb.zonedInstant(nowRange),
            ) { nowDiff, now ->
                val value = valueFromInstant(now + nowDiff)
                withClue("value = $value") {
                    formatAndTestNextTick(value, now, format)
                }
            }
        }
    }
}

suspend fun <T> FunSpecContainerScope.nextTickPredictsChangeTestMaybeZoned(
    arb: Arb<MaybeZoned<T>>,
    valueFromInstant: (Zoned<Instant>) -> T,
    format: (MaybeZoned<T>, now: Zoned<Instant>) -> TickingValue<String>,
    nowRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
) {
    nextTickPredictsChangeTest(
        arb = arb,
        valueFromInstant = { zonedInstant ->
            Arb.maybeZoned(zonedInstant.value)
                .map { (instant, tz) -> MaybeZoned(valueFromInstant(Zoned(instant, tz ?: zonedInstant.timeZone)), tz) }
                .next()
        },
        format = format,
        nowRange = nowRange,
    )
}

private fun <T> testNextTickPredictsChange(
    value: T,
    formatted: TickingValue<String>,
    now: Zoned<Instant>,
    format: (T, now: Zoned<Instant>) -> TickingValue<String>,
) {
    if (formatted.nextTick == null) {
        withClue("nextTick is null, so format in the very far future (100 years) should yield same value") {
            format(value, now + (365 * 100).days) shouldBe formatted
        }
    } else {
        withClue("nextTick (${formatted.nextTick}) would be @ ${now.value.plus(formatted.nextTick)}") {
            withClue("format at 1 nanosecond before nextTick should yield same relative value") {
                format(value, now + (formatted.nextTick - 1.nanoseconds)) should {
                    it.value shouldBe formatted.value
                    it.nextTick shouldBe 1.nanoseconds
                }
            }

            withClue("format at nextTick should yield different relative value") {
                format(value, now + formatted.nextTick) should {
                    it.value shouldNotBe formatted.value
                }
            }
        }
    }
}

fun <T> formatAndTestNextTick(
    value: T,
    now: Zoned<Instant>,
    format: (T, now: Zoned<Instant>) -> TickingValue<String>,
): TickingValue<String> {
    return format(value, now).also { formatted ->
        testNextTickPredictsChange(value, formatted, now, format)
    }
}

fun Arb.Companion.kotlinTimeZone() = Arb.zoneId().map { it.toKotlinTimeZone() }
fun Arb.Companion.kotlinLocalDateTime() = Arb.localDateTime().map { it.toKotlinLocalDateTime() }

fun Arb.Companion.zonedInstant(instantRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE) = arbitrary {
    Zoned(
        value = Arb.kotlinInstant(instantRange).bind(),
        timeZone = TimeZone.UTC,//Arb.kotlinTimeZone().bind(),
    )
}

fun <T> Arb.Companion.maybeZoned(value: Arb<T>) = arbitrary {
    MaybeZoned(
        value = value.bind(),
        timeZone = Arb.choose(
            10 to listOf(TimeZone.UTC).exhaustive().toArb(),//Arb.kotlinTimeZone(),
            1 to listOf(null).exhaustive().toArb(),
        ).bind(),
    )
}

fun <T> Arb.Companion.maybeZoned(value: T) = maybeZoned(listOf(value).exhaustive().toArb())
