package io.github.couchtracker.db.profile

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

    init {
        context("test") {
            val driver = inMemorySqliteDriver()
            ProfileData.Schema.create(driver)
            val profileData = get<ProfileData> { parametersOf(driver) }

            test("select when empty") {
                profileData.metadataQueries.select(TEST_KEY).executeAsOneOrNull().shouldBeNull()
            }
            test("upsert when empty") {
                shouldNotThrowAny {
                    profileData.metadataQueries.upsert(TEST_KEY, "testValue1")
                }
            }
            test("select when not empty") {
                profileData.metadataQueries.select(TEST_KEY).executeAsOneOrNull() shouldBe "testValue1"
            }
            test("upsert when not empty") {
                profileData.metadataQueries.upsert(TEST_KEY, "testValue2")
                profileData.metadataQueries.select(TEST_KEY).executeAsOneOrNull() shouldBe "testValue2"
            }
            test("delete") {
                profileData.metadataQueries.delete(TEST_KEY)
                profileData.metadataQueries.select(TEST_KEY).executeAsOneOrNull().shouldBeNull()
            }
        }
    }

    override fun extensions() = listOf(KoinExtension(module = ProfileDbModule.value, mode = KoinLifecycleMode.Root))
}
