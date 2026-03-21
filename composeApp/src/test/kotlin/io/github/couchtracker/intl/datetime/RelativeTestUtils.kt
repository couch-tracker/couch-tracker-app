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
import io.kotest.property.arbitrary.zoneId
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toKotlinTimeZone
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Instant

data class WithNowFormatParams<T>(val value: T, val now: Zoned<Instant>) {
    operator fun plus(duration: Duration) = WithNowFormatParams(value, now + duration)
}

fun <T> Arb.Companion.withNowFormatParams(
    value: Arb<T>,
    nowRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
) = arbitrary {
    WithNowFormatParams(
        value = value.bind(),
        now = Arb.zonedInstant(nowRange).bind(),
    )
}

/**
 * Runs tests that validate that the computed [TickingValue.nextTick] is correctly predicting a change in formatting.
 *
 * Two similar but distinct fuzzy tests are performed:
 * - one using arbitrary [P] values from the given [arbitraryArb]
 * - one using an arbitrary [P] values from [smallArb], which receives a small [Duration] as input (between -100 and 100 days), to be used
 *   to calculate the params. This duration is meant to be used as a way to create more realistic params to format.
 *
 * The second case guarantees that more realistic cases (where now and the value to format are close) are covered.
 */
suspend fun <P> FunSpecContainerScope.nextTickPredictsChangeTest(
    arbitraryArb: Arb<P>,
    smallArb: (Arb<Duration>) -> Arb<P>,
    advanceBy: P.(Duration) -> P,
    format: (P) -> TickingValue<String>,
) {
    context("correctly predicts date change") {
        // This first test just checks arbitrary values
        // However, these values will likely be very far apart, and wouldn't necessarily test all relevant cases
        test("arbitrary values") {
            checkAll(arbitraryArb) { params ->
                val formatted = format(params)
                testNextTickPredictsChange(params, formatted, advanceBy, format)
            }
        }

        // This test instead computes the value from a small amount of time away from now, ensuring more realistic cases are also covered
        test("small diff value") {
            checkAll(
                smallArb(Arb.duration(-100.days..100.days)),
            ) { params ->
                val formatted = format(params)
                testNextTickPredictsChange(params, formatted, advanceBy, format)
            }
        }
    }
}

/**
 * Version of [nextTickPredictsChangeTest] to test a formatter accepting a [T] and a [Zoned]<[Instant]>.
 */
suspend fun <T> FunSpecContainerScope.nextTickPredictsChangeTestWithNow(
    arbitraryArb: Arb<T>,
    smallArb: (Zoned<Instant>) -> Arb<T>,
    format: (T, now: Zoned<Instant>) -> TickingValue<String>,
    nowRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
) = nextTickPredictsChangeTest(
    arbitraryArb = Arb.withNowFormatParams(arbitraryArb, nowRange),
    smallArb = { diff ->
        arbitrary {
            val now = Arb.zonedInstant(nowRange).bind()
            WithNowFormatParams(
                value = smallArb(now + diff.bind()).bind(),
                now = now,
            )
        }
    },
    advanceBy = WithNowFormatParams<T>::plus,
    format = { format(it.value, it.now) },
)

suspend fun <T> FunSpecContainerScope.nextTickPredictsChangeTestMaybeZoned(
    arbitraryArb: Arb<MaybeZoned<T>>,
    smallArb: (Zoned<Instant>) -> Arb<T>,
    format: (MaybeZoned<T>, now: Zoned<Instant>) -> TickingValue<String>,
    nowRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE,
) {
    nextTickPredictsChangeTestWithNow(
        arbitraryArb = arbitraryArb,
        smallArb = { zonedInstant ->
            arbitrary {
                val (instant, tz) = Arb.maybeZoned(zonedInstant.value).bind()
                MaybeZoned(smallArb(Zoned(instant, tz ?: zonedInstant.timeZone)).bind(), tz)
            }
        },
        format = format,
        nowRange = nowRange,
    )
}

private fun <P> testNextTickPredictsChange(
    value: P,
    formatted: TickingValue<String>,
    advanceBy: P.(Duration) -> P,
    format: (P) -> TickingValue<String>,
) {
    if (formatted.nextTick == null) {
        withClue("nextTick is null, so format in the very far future (100 years) should yield same value") {
            format(value.advanceBy((365 * 100).days)) shouldBe formatted
        }
    } else {
        withClue("nextTick is ${formatted.nextTick} would be @ ${value.advanceBy(formatted.nextTick - 1.nanoseconds)}") {
            withClue("format at 1 nanosecond before nextTick should yield same relative value") {
                format(value.advanceBy(formatted.nextTick - 1.nanoseconds)) should {
                    it.value shouldBe formatted.value
                    it.nextTick shouldBe 1.nanoseconds
                }
            }

            withClue("format at nextTick should yield different relative value") {
                format(value.advanceBy(formatted.nextTick)) should {
                    it.value shouldNotBe formatted.value
                }
            }
        }
    }
}

fun <P> formatAndTestNextTick(
    params: P,
    advanceBy: P.(Duration) -> P,
    format: (P) -> TickingValue<String>,
): TickingValue<String> {
    return format(params).also { formatted ->
        testNextTickPredictsChange(
            value = params,
            formatted = formatted,
            advanceBy = advanceBy,
            format = format,
        )
    }
}

fun <T> formatAndTestNextTick(
    value: T,
    now: Zoned<Instant>,
    format: (T, now: Zoned<Instant>) -> TickingValue<String>,
): TickingValue<String> {
    val params = WithNowFormatParams(value, now)
    return formatAndTestNextTick(
        params = params,
        advanceBy = WithNowFormatParams<T>::plus,
        format = { format(it.value, it.now) },
    )
}

fun Arb.Companion.kotlinTimeZone() = Arb.zoneId().map { it.toKotlinTimeZone() }
fun Arb.Companion.kotlinLocalDateTime() = Arb.localDateTime().map { it.toKotlinLocalDateTime() }

fun Arb.Companion.zonedInstant(instantRange: KotlinInstantRange = Instant.DISTANT_PAST..Instant.DISTANT_FUTURE) = arbitrary {
    Zoned(
        value = Arb.kotlinInstant(instantRange).bind(),
        timeZone = Arb.kotlinTimeZone().bind(),
    )
}

fun <T> Arb.Companion.maybeZoned(value: Arb<T>) = arbitrary {
    MaybeZoned(
        value = value.bind(),
        timeZone = Arb.choose(
            10 to Arb.kotlinTimeZone(),
            1 to listOf(null).exhaustive().toArb(),
        ).bind(),
    )
}

fun <T> Arb.Companion.maybeZoned(value: T) = maybeZoned(listOf(value).exhaustive().toArb())
