package io.github.couchtracker.db.profile

import app.cash.sqldelight.ColumnAdapter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import io.mockk.verify

private const val TEST_KEY = "testKey"
private const val TEST_VALUE = "testValue"

class MetadataItemTest : FunSpec(
    {
        val columnAdapter = mockk<ColumnAdapter<TestData, String>> {
            every { decode(TEST_VALUE) } returns TestData
            every { encode(TestData) } returns TEST_VALUE
        }
        context("constructor fails for invalid keys") {
            withData(
                nameFn = { it.ifEmpty { "<empty>" }.ifBlank { "<blank>" } },
                listOf("", "  ", "Abc", "abc xyz", " abc", "abc "),
            ) { key ->
                shouldThrow<IllegalArgumentException> { MetadataItem(key = key, columnAdapter) }
            }
        }
        context("getValue()") {
            test("when metadata is present") {
                val db = mockk<ProfileData> {
                    every { metadataQueries.select(TEST_KEY) } returns mockk {
                        every { executeAsOneOrNull() } returns TEST_VALUE
                    }
                }
                val metadataItem = MetadataItem(key = TEST_KEY, columnAdapter)
                metadataItem.getValue(db) shouldBe TestData
            }
            test("when metadata is missing") {
                val db = mockk<ProfileData> {
                    every { metadataQueries.select(TEST_KEY) } returns mockk {
                        every { executeAsOneOrNull() } returns null
                    }
                }
                val metadataItem = MetadataItem(key = TEST_KEY, columnAdapter)
                metadataItem.getValue(db).shouldBeNull()
            }
        }
        context("setValue()") {
            test("when value is set") {
                val db = mockk<ProfileData>(relaxed = true)
                val metadataItem = MetadataItem(key = TEST_KEY, columnAdapter)
                metadataItem.setValue(db, TestData)
                verify(exactly = 1) { db.metadataQueries.upsert(TEST_KEY, TEST_VALUE) }
            }
            test("when value is null") {
                val db = mockk<ProfileData>(relaxed = true)
                val metadataItem = MetadataItem(key = TEST_KEY, columnAdapter)
                metadataItem.setValue(db, null)
                verify(exactly = 1) { db.metadataQueries.delete(TEST_KEY) }
            }
        }
        test("delete()") {
            // Just testing that we call setValue()
            val metadataItem = spyk(MetadataItem(key = TEST_KEY, columnAdapter)) {
                every { setValue(any(), any()) } just runs
            }
            val db = mockk<ProfileData>()
            metadataItem.delete(db)
            verify(exactly = 1) { metadataItem.setValue(db, value = null) }
        }
    },
)

private data object TestData
