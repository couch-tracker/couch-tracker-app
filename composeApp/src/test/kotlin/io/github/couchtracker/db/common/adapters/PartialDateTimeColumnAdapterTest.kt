package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.user.model.partialtime.PartialDateTime
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockkObject
import io.mockk.unmockkObject

class PartialDateTimeColumnAdapterTest : FunSpec(
    {
        beforeTest {
            mockkObject(PartialDateTime)
        }
        afterTest {
            unmockkObject(PartialDateTime)
        }

        context("test with mocks") {
            testColumnAdapterWithMocks(
                adapter = PartialDateTimeColumnAdapter,
                fakeDatabaseValue = "test",
                serialize = { serialize() },
                parse = { PartialDateTime.parse(it) },
            )
        }
    },
)
