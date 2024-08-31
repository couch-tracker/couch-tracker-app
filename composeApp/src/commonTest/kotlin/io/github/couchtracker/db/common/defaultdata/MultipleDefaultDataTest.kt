package io.github.couchtracker.db.common.defaultdata

import app.cash.sqldelight.Transacter
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.IsolationMode
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifySequence

class MultipleDefaultDataTest : FunSpec(
    {
        isolationMode = IsolationMode.InstancePerTest

        val db = mockk<Transacter>()
        val (data1, data2, data3) = List(3) {
            mockk<DefaultData<Transacter>>(relaxed = true)
        }

        context("insert()") {
            test("calls all datas") {
                MultipleDefaultData(listOf(data1, data2, data3)).insert(db)

                verifySequence {
                    data1.insert(db)
                    data2.insert(db)
                    data3.insert(db)
                }
            }
            test("fails if one data fails") {
                val exception = Exception("error")
                every { data2.insert(db) } throws exception
                val data = MultipleDefaultData(listOf(data1, data2, data3))
                shouldThrow<Exception> { data.insert(db) } shouldBeSameInstanceAs exception
                verifySequence {
                    data1.insert(db)
                    data2.insert(db)
                }
                confirmVerified(data1, data2, data3)
            }
        }
        context("upgradeTo()") {
            val newVersion = 123
            test("calls all datas") {
                MultipleDefaultData(listOf(data1, data2, data3)).upgradeTo(db, newVersion)

                verifySequence {
                    data1.upgradeTo(db, newVersion)
                    data2.upgradeTo(db, newVersion)
                    data3.upgradeTo(db, newVersion)
                }
            }
            test("fails if one data fails") {
                val exception = Exception("error")
                every { data2.upgradeTo(db, newVersion) } throws exception
                val data = MultipleDefaultData(listOf(data1, data2, data3))
                shouldThrow<Exception> { data.upgradeTo(db, newVersion) } shouldBeSameInstanceAs exception
                verifySequence {
                    data1.upgradeTo(db, newVersion)
                    data2.upgradeTo(db, newVersion)
                }
                verify(exactly = 0) { data3.upgradeTo(db, newVersion) }
                confirmVerified(data1, data2, data3)
            }
        }
    },
)
