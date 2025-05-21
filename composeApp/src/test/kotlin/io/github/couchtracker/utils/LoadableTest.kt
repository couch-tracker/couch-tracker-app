package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.property.checkAll
import io.kotest.property.exhaustive.exhaustive
import io.kotest.property.exhaustive.filter

class LoadableTest : FunSpec(
    {
        context("Flat Map") {
            // See https://wiki.haskell.org/Monad_laws
            context("Obeys Monad's laws") {
                test("Left identity") {
                    checkAll(ARBITRARY_VALUE) { value ->
                        checkAll(ARBITRARY_FUNCTION_LOADABLE) { f ->
                            Loadable.Loaded(value).flatMap(f) shouldBe f(value)
                        }
                    }
                }
                test("Right identity") {
                    checkAll(ARBITRARY_LOADABLE) { loadable ->
                        loadable.flatMap { Loadable.Loaded(it) } shouldBe loadable
                    }
                }
                test("Associativity") {
                    checkAll(ARBITRARY_LOADABLE) { loadable ->
                        checkAll(ARBITRARY_FUNCTION_LOADABLE) { f ->
                            checkAll(ARBITRARY_FUNCTION_LOADABLE) { g ->
                                loadable.flatMap(f).flatMap(g) shouldBe loadable.flatMap { f(it).flatMap(g) }
                            }
                        }
                    }
                }
            }
            test("Short-circuits Loading") {
                Loadable.Loading.flatMap<Nothing, Nothing, Nothing> {
                    throw IllegalStateException()
                } shouldBe Loadable.Loading
            }
            test("Short-circuits Error") {
                val arbitraryError = ARBITRARY_LOADABLE.filter { it is Loadable.Error }
                checkAll(arbitraryError) { error ->
                    error.flatMap<Any?, Nothing, Any?> {
                        throw IllegalStateException()
                    } shouldBe error
                }
            }
        }
        context("Map") {
            test("Is equivalent to Flat Map") {
                checkAll(ARBITRARY_LOADABLE) { loadable ->
                    checkAll(ARBITRARY_FUNCTION_VALUE) { f ->
                        loadable.map(f) shouldBe loadable.flatMap { Loadable.Loaded(f(it)) }
                    }
                }
            }
        }
    },
)

private val ARBITRARY_VALUE = listOf<Any?>(
    1,
    2,
    "A",
    "B",
).exhaustive()

private val ARBITRARY_FUNCTION_VALUE = listOf<(Any?) -> Any?>(
    // Identity
    { x -> x },
    // Constants
    { _ -> 1 },
    { _ -> 2 },
    { _ -> "A" },
    { _ -> "B" },
    // Variable
    { x -> "$x, but better" },
    { x -> "$x, but worst" },
).exhaustive()

private val ARBITRARY_FUNCTION_LOADABLE = listOf<(Any?) -> Loadable<Any?, String>>(
    // Identity
    { x -> Loadable.Loaded(x) },
    // Constants
    { _ -> Loadable.Loading },
    { _ -> Loadable.Error("Example Error A") },
    { _ -> Loadable.Loaded(1) },
    { _ -> Loadable.Loaded(2) },
    { _ -> Loadable.Loaded("A") },
    { _ -> Loadable.Loaded("B") },
    // Variable
    { x -> Loadable.Loaded("$x, but better") },
    { x -> Loadable.Loaded("$x, but worst") },
).exhaustive()

private val ARBITRARY_LOADABLE = listOf<Loadable<Any?, String>>(
    Loadable.Loading,
    Loadable.Error("Example Error A"),
    Loadable.Error("Example Error B"),
    Loadable.Loaded(1),
    Loadable.Loaded(2),
    Loadable.Loaded("A"),
    Loadable.Loaded("B"),
).exhaustive()
