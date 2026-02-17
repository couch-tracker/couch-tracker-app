package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.scopes.FunSpecContainerScope
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

/**
 * @param databaseValues list of values that can be parsed from the DB into [decodedValue]. The first item in the list is the expected
 * serialized value
 * @param decodedValue the expected decoded value for each element in [databaseValues]
 */
data class ColumnAdapterTest<T : Any, S>(
    val databaseValues: List<S>,
    val decodedValue: T,
) {
    val encodedValue get() = databaseValues.first()

    init {
        require(databaseValues.isNotEmpty())
    }

    constructor(databaseValue: S, decodedValue: T) : this(listOf(databaseValue), decodedValue)
}

suspend fun <T : Any, S> FunSpecContainerScope.testColumnAdapter(
    columnAdapter: ColumnAdapter<T, S>,
    valid: List<ColumnAdapterTest<T, S>>,
    invalid: List<S>,
) {
    context("valid values") {
        context("should decode correctly") {
            withData(nameFn = { it.decodedValue.toString() }, valid) { testCase ->
                withData(nameFn = { it.toString().ifEmpty { "<empty>" } }, testCase.databaseValues) { databaseValue ->
                    columnAdapter.decode(databaseValue) shouldBe testCase.decodedValue
                }
            }
        }
        context("should encode correctly") {
            withData(nameFn = { it.decodedValue.toString() }, valid) { testCase ->
                columnAdapter.encode(testCase.decodedValue) shouldBe testCase.encodedValue
            }
        }
    }

    context("invalid values") {
        withData(nameFn = { it.toString().ifEmpty { "<empty>" }.ifBlank { "<blank>" } }, invalid) { value ->
            shouldThrowAny {
                columnAdapter.decode(value)
            }
        }
    }
}

/**
 * Utility function to check that a [ColumnAdapter] behaves very simply: calls a function to serialize and another one to deserialize.
 * Furthermore, exceptions should be propagated.
 *
 * @param [adapter] the adapter to test
 * @param fakeDatabaseValue a fake database value to use to mock the encoding/decoding. We can't mock this value because it's usually
 * "core" type like String or Int, which cannot be mocked.
 * @param serialize callback that calls the serialize function that it's expected on the given [T] mock.
 * @param parse callback that calls the correct parsing function with the given [S] database value.
 * Note: since this function cannot know what "thing" to mock in order to mock the parsing function (`mockkStatic`, `mockObject`, etc.), it
 * is responsibility of the caller to mock/unmock the correct thing on the beforeTest/afterTest callbacks so that the [parse] callback can
 * be used inside an [every] stubbing.
 */
suspend inline fun <reified T : Any, reified S : Any> FunSpecContainerScope.testColumnAdapterWithMocks(
    adapter: ColumnAdapter<T, S>,
    fakeDatabaseValue: S,
    noinline serialize: T.() -> S,
    noinline parse: (S) -> T,
) {
    context("encode()") {
        test("passes through return value of the serialization function") {
            val mockedValue = mockk<T> {
                every { serialize() } returns fakeDatabaseValue
            }
            adapter.encode(mockedValue) shouldBe fakeDatabaseValue
            verify(exactly = 1) { mockedValue.serialize() }
        }
        test("passes through exceptions of the serialization function") {
            val exception = Exception("serialization error")
            val mockedValue = mockk<T> {
                every { serialize(this@mockk) } throws exception
            }
            val thrown = shouldThrowAny {
                adapter.encode(mockedValue)
            }
            thrown shouldBeSameInstanceAs exception
            verify(exactly = 1) { mockedValue.serialize() }
        }
    }
    context("decode()") {
        test("passes through return value of the parse function") {
            val mockedValue = mockk<T>()
            every { parse(fakeDatabaseValue) } returns mockedValue
            adapter.decode(fakeDatabaseValue) shouldBeSameInstanceAs mockedValue
            verify(exactly = 1) { parse(fakeDatabaseValue) }
        }
        test("passes through exceptions of the parse function") {
            val exception = Exception("parse error")
            every { parse(fakeDatabaseValue) } throws exception
            val thrown = shouldThrowAny {
                adapter.decode(fakeDatabaseValue)
            }
            thrown shouldBeSameInstanceAs exception
            verify(exactly = 1) { parse(fakeDatabaseValue) }
        }
    }
}
