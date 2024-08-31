package io.github.couchtracker.db.user

import io.github.couchtracker.db.inMemorySqliteDriver
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.core.spec.style.FunSpec
import io.kotest.koin.KoinExtension
import io.kotest.koin.KoinLifecycleMode
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import org.koin.test.KoinTest

private const val TEST_KEY = "testKey"

class MetadataQueriesTest : KoinTest, FunSpec() {

    override fun extensions() = listOf(KoinExtension(module = UserDbModule, mode = KoinLifecycleMode.Root))

    init {
        context("test") {
            val driver = inMemorySqliteDriver()
            UserData.Schema.create(driver)
            val userData = get<UserData> { parametersOf(driver) }

            test("select when empty") {
                userData.metadataQueries.select(TEST_KEY).executeAsOneOrNull().shouldBeNull()
            }
            test("upsert when empty") {
                shouldNotThrowAny {
                    userData.metadataQueries.upsert(TEST_KEY, "testValue1")
                }
            }
            test("select when not empty") {
                userData.metadataQueries.select(TEST_KEY).executeAsOneOrNull() shouldBe "testValue1"
            }
            test("upsert when not empty") {
                userData.metadataQueries.upsert(TEST_KEY, "testValue2")
                userData.metadataQueries.select(TEST_KEY).executeAsOneOrNull() shouldBe "testValue2"
            }
            test("delete") {
                userData.metadataQueries.delete(TEST_KEY)
                userData.metadataQueries.select(TEST_KEY).executeAsOneOrNull().shouldBeNull()
            }
        }
    }
}
