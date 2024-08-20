package io.github.couchtracker.db.common.adapters

import io.kotest.core.spec.style.FunSpec
import io.mockk.mockkObject
import io.mockk.unmockkObject
import kotlinx.datetime.Instant

class InstantColumnAdapterTest : FunSpec(
    {
        beforeTest {
            mockkObject(Instant)
        }
        afterTest {
            unmockkObject(Instant)
        }

        context("test with mocks") {
            testColumnAdapterWithMocks(
                adapter = InstantColumnAdapter,
                fakeDatabaseValue = "test",
                serialize = { toString() },
                parse = { Instant.parse(it) },
            )
        }
    },
)
