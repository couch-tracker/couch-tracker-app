package io.github.couchtracker.utils

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.core.tuple
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class TickingValueTest : FunSpec(
    {
        context("constructor") {
            context("fails with invalid nextTick values") {
                withTests(
                    Duration.ZERO,
                    Duration.INFINITE,
                    -Duration.INFINITE,
                    -1.nanoseconds,
                    -5.days,
                ) { nextTick ->
                    shouldThrow<IllegalArgumentException> {
                        TickingValue("", nextTick = nextTick)
                    }
                }
            }
        }

        context("combine") {
            withTests(
                tuple(
                    TickingValue("hello", 1.seconds),
                    TickingValue("world", 500.milliseconds),
                    TickingValue("helloworld", 500.milliseconds),
                ),
                tuple(
                    TickingValue("ciao", 1.days),
                    TickingValue("mondo", null),
                    TickingValue("ciaomondo", 1.days),
                ),
                tuple(
                    TickingValue("x", null),
                    TickingValue("y", null),
                    TickingValue("xy", null),
                ),
            ) { (t1, t2, expected) ->
                t1.combine(t2, transform = { a, b -> a + b }) shouldBe expected
            }
        }

        context("withNextTickAtMost") {
            withTests(
                nameFn = { "${it.a.nextTick}.withNextTickAtMost(${it.b}) = ${it.c.nextTick}" },
                tuple(
                    TickingValue("ciao", 1.days),
                    50.minutes,
                    TickingValue("ciao", 50.minutes),
                ),
                tuple(
                    TickingValue("ciao", 5.seconds),
                    7.seconds,
                    TickingValue("ciao", 5.seconds),
                ),
                tuple(
                    TickingValue("hola", 1.seconds),
                    null,
                    TickingValue("hola", 1.seconds),
                ),
                tuple(
                    TickingValue("hallo", null),
                    null,
                    TickingValue("hallo", null),
                ),
            ) { (tickingValue, nextTick, expected) ->
                tickingValue.withNextTickAtMost(nextTick = nextTick) shouldBe expected
            }
        }
    },
)
