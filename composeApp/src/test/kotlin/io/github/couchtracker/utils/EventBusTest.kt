package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.core.test.config.DefaultTestConfig
import io.kotest.engine.coroutines.backgroundScope
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds

class EventBusTest : FunSpec(
    {
        defaultTestConfig = DefaultTestConfig(
            coroutineTestScope = true,
            timeout = 1.seconds,
        )
        test("subscribe emits the filter value immediately") {
            val buffer = EventBus<Int>(backgroundScope)
            buffer.subscribe().take(1).toList() shouldBe listOf(null)
        }

        test("publish delivers matching events to subscribers") {
            val buffer = EventBus<String>(backgroundScope)
            val results = async {
                buffer.subscribe().take(4).toList()
            }
            launch {
                buffer.publish("event-1")
                buffer.publish("event-2")
                buffer.publish("event-3")
            }
            results.await() shouldBe listOf(null, "event-1", "event-2", "event-3")
        }

        test("publish can be called from inside a subscriber without deadlocking") {
            val buffer = EventBus<String>(backgroundScope)
            val results = buffer
                .subscribe()
                .onEach {
                    buffer.publish("echo (prev=$it)")
                }
                .take(3)
                .toList()

            results shouldBe listOf(null, "echo (prev=null)", "echo (prev=echo (prev=null))")
        }

        test("multiple subscribers") {
            val buffer = EventBus<String>(backgroundScope)
            val results1 = async {
                buffer.subscribe().take(3).toList()
            }
            val results2 = async {
                buffer.subscribe().take(3).toList()
            }
            val results3 = async {
                buffer.subscribe().take(5).toList()
            }
            launch {
                buffer.publish("key-1")
                buffer.publish("key-2")
            }
            results1.await() shouldBe listOf(null, "key-1", "key-2")
            results2.await() shouldBe listOf(null, "key-1", "key-2")

            val results4 = async {
                buffer.subscribe().take(3).toList()
            }
            launch {
                buffer.publish("key-3")
                buffer.publish("key-4")
            }
            results3.await() shouldBe listOf(null, "key-1", "key-2", "key-3", "key-4")
            results4.await() shouldBe listOf(null, "key-3", "key-4")
        }
    },
)
