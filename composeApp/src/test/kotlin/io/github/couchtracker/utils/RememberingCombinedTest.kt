package io.github.couchtracker.utils

import app.cash.turbine.test
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableSharedFlow

class RememberingCombinedTest : FunSpec(
    {

        test("emits immediately when there are no keys") {
            val input = MutableSharedFlow<Set<Int>>()

            input
                .rememberingCombined(
                    keys = { it },
                    flowToRemember = { error("Should not be called") },
                    f = { _, values: Map<Int, Nothing> -> values },
                )
                .test {
                    input.emit(emptySet())

                    awaitItem() shouldBe emptyMap()
                }
        }

        test("waits until all flows have emitted") {
            val input = MutableSharedFlow<Set<Int>>()

            val flows = mapOf(
                1 to MutableSharedFlow<String>(),
                2 to MutableSharedFlow<String>(),
            )

            input
                .rememberingCombined(
                    keys = { it },
                    flowToRemember = { flows.getValue(it) },
                    f = { _, values -> values.toMap() },
                )
                .test {
                    input.emit(setOf(1, 2))

                    expectNoEvents()

                    flows.getValue(1).emit("one")
                    expectNoEvents()

                    flows.getValue(2).emit("two")

                    awaitItem() shouldBe mapOf(
                        1 to "one",
                        2 to "two",
                    )
                }
        }

        test("reuses flows for preserved keys") {
            val input = MutableSharedFlow<Set<Int>>()

            val created = mutableListOf<Int>()
            val flows = mutableMapOf<Int, MutableSharedFlow<String>>()

            input
                .rememberingCombined(
                    keys = { it },
                    flowToRemember = { key ->
                        created += key
                        flows.getOrPut(key) { MutableSharedFlow() }
                    },
                    f = { _, values -> values.toMap() },
                )
                .test {
                    input.emit(setOf(1))
                    flows.getValue(1).emit("a")
                    awaitItem() shouldBe mapOf(1 to "a")

                    input.emit(setOf(1))
                    awaitItem() shouldBe mapOf(1 to "a")

                    created shouldBe listOf(1)
                }
        }

        test("re-emits when a remembered flow updates") {
            val input = MutableSharedFlow<Set<Int>>()
            val flow = MutableSharedFlow<String>()

            input
                .rememberingCombined(
                    keys = { it },
                    flowToRemember = { flow },
                    f = { _, values -> values.getValue(1) },
                )
                .test {
                    input.emit(setOf(1))

                    flow.emit("a")
                    awaitItem() shouldBe "a"

                    flow.emit("b")
                    awaitItem() shouldBe "b"
                }
        }

        test("adding a key keeps previous values") {
            val input = MutableSharedFlow<Set<Int>>()

            val flows = mapOf(
                1 to MutableSharedFlow<String>(),
                2 to MutableSharedFlow<String>(),
            )

            input
                .rememberingCombined(
                    keys = { it },
                    flowToRemember = { flows.getValue(it) },
                    f = { _, values -> values.toMap() },
                )
                .test {
                    input.emit(setOf(1))
                    flows.getValue(1).emit("one")

                    awaitItem() shouldBe mapOf(1 to "one")

                    input.emit(setOf(1, 2))
                    expectNoEvents()

                    flows.getValue(2).emit("two")

                    awaitItem() shouldBe mapOf(
                        1 to "one",
                        2 to "two",
                    )
                }
        }
    },
)
