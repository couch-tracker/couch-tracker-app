package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.runTest
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalCoroutinesApi::class)
class FlowTest : FunSpec(
    {
        test("biFork") {
            val scheduler = TestCoroutineScheduler()
            val testDispatcher = StandardTestDispatcher(scheduler)
            runTest(testDispatcher) {
                val testFlow = flow {
                    repeat(4) {
                        emit(it)
                        delay(3.seconds)
                    }
                }
                val forked = testFlow.biFork(
                    // Returns the numbers after a delay
                    fork1 = { inputFlow ->
                        inputFlow.map {
                            delay(1.seconds)
                            it
                        }
                    },
                    // Returns the numbers, doubled, after a delay
                    fork2 = { inputFlow ->
                        inputFlow.map {
                            delay(2.seconds)
                            it * 2
                        }
                    },
                    // Merges the stream only when the two forks are consistent
                    merge = { a, b ->
                        if (b == 2 * a) {
                            emit(Triple(scheduler.currentTime, a, b))
                        }
                    },
                )
                val result = forked.toList()
                result shouldBe listOf(
                    Triple(2_000L, 0, 0),
                    Triple(5_000L, 1, 2),
                    Triple(8_000L, 2, 4),
                    Triple(11_000L, 3, 6),
                )
            }
        }
    },
)
