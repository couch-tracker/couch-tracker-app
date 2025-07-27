package io.github.couchtracker.utils

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withData
import io.kotest.inspectors.forAny
import io.kotest.matchers.collections.shouldBeIn
import io.kotest.matchers.shouldBe
import io.kotest.property.Arb
import io.kotest.property.arbitrary.duration
import io.kotest.property.arbitrary.enum
import io.kotest.property.checkAll
import kotlin.math.absoluteValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.microseconds
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class DurationTest : FunSpec(
    {
        context("unitPart") {
            val duration = 69.days + 13.hours + 40.minutes + 21.seconds + 320.milliseconds + 255.microseconds + 420.nanoseconds
            withData(
                DurationUnit.DAYS to 69,
                DurationUnit.HOURS to 13,
                DurationUnit.MINUTES to 40,
                DurationUnit.SECONDS to 21,
                DurationUnit.MILLISECONDS to 320,
                DurationUnit.MICROSECONDS to 255,
                DurationUnit.NANOSECONDS to 420,
            ) { (unit, expected) ->
                withClue("positive duration") {
                    duration.unitPart(unit) shouldBe expected.toLong()
                }

                withClue("negative duration") {
                    (-duration).unitPart(unit) shouldBe -expected.toLong()
                }
            }

            context("zero duration") {
                withData(DurationUnit.entries) { unit ->
                    Duration.ZERO.unitPart(unit) shouldBe 0
                }
            }
        }

        context("remainderUntilNextUnitBoundary") {
            context("positive values") {
                withData(
                    nameFn = { "${it.a}, ${it.b} -> ${it.c}" },
                    tuple(1.hours, DurationUnit.MINUTES, 1.nanoseconds),
                    tuple(3.hours + 20.minutes, DurationUnit.HOURS, 20.minutes + 1.nanoseconds),
                    tuple(1.hours, DurationUnit.HOURS, 1.nanoseconds),
                    tuple(5.seconds + 230.milliseconds, DurationUnit.SECONDS, 230.milliseconds + 1.nanoseconds),
                    tuple(5.seconds, DurationUnit.MILLISECONDS, 1.nanoseconds),
                    tuple(5.minutes, DurationUnit.HOURS, 5.minutes),
                    tuple(5.minutes, DurationUnit.DAYS, 5.minutes),
                ) { (duration, unit, expected) ->
                    duration.remainderUntilNextUnitBoundary(unit) shouldBe expected
                }
            }
            context("zero duration") {
                withData(
                    nameFn = { "${it.first} -> ${it.second}" },
                    DurationUnit.NANOSECONDS to 1.nanoseconds,
                    DurationUnit.MILLISECONDS to 1.milliseconds,
                    DurationUnit.SECONDS to 1.seconds,
                    DurationUnit.MINUTES to 1.minutes,
                    DurationUnit.HOURS to 1.hours,
                    DurationUnit.DAYS to 1.days,
                ) { (unit, expected) ->
                    Duration.ZERO.remainderUntilNextUnitBoundary(unit) shouldBe expected
                }
            }
            context("negative values") {
                withData(
                    nameFn = { "${it.a}, ${it.b} -> ${it.c}" },
                    tuple(1.hours, DurationUnit.MINUTES, 1.minutes),
                    tuple(1.hours + 20.minutes, DurationUnit.HOURS, 40.minutes),
                    tuple(1.hours, DurationUnit.HOURS, 1.hours),
                    tuple(5.seconds + 230.milliseconds, DurationUnit.SECONDS, 770.milliseconds),
                    tuple(5.seconds, DurationUnit.MILLISECONDS, 1.milliseconds),
                    tuple(5.minutes, DurationUnit.HOURS, 55.minutes),
                    tuple(5.minutes, DurationUnit.DAYS, 23.hours + 55.minutes),
                ) { (duration, unit, expected) ->
                    (-duration).remainderUntilNextUnitBoundary(unit) shouldBe expected
                }
            }

            test("check that the desired unit actually changes") {
                checkAll(iterations = 10_000, Arb.duration(), Arb.enum<DurationUnit>()) { duration, unit ->
                    val prevUnit = duration.unitPart(unit).absoluteValue
                    val remainder = duration.remainderUntilNextUnitBoundary(unit)
                    val newDuration = duration - remainder
                    val nextUnit = newDuration.unitPart(unit).absoluteValue

                    listOf(
                        { nextUnit shouldBeIn setOfNotNull(unit.decrease(prevUnit), unit.increase(prevUnit)) },
                        { newDuration shouldBe Duration.ZERO },
                    ).forAny { it() }
                }
            }
        }
    },
)

private val DurationUnit.maxValue
    get() = when (this) {
        DurationUnit.NANOSECONDS -> 1000
        DurationUnit.MICROSECONDS -> 1000
        DurationUnit.MILLISECONDS -> 1000
        DurationUnit.SECONDS -> 60
        DurationUnit.MINUTES -> 60
        DurationUnit.HOURS -> 24
        DurationUnit.DAYS -> null
    }

private fun DurationUnit.increase(value: Long): Long {
    val next = value + 1
    return maxValue?.let { next % it } ?: next
}

private fun DurationUnit.decrease(value: Long): Long? {
    val prev = value - 1
    return if (prev < 0) {
        maxValue?.let { prev + it }
    } else {
        prev
    }
}
