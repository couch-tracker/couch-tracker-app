package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.utils.parseUri
import io.kotest.core.spec.style.FunSpec
import io.mockk.mockkStatic
import io.mockk.unmockkStatic

class UriColumnAdapterTest : FunSpec(
    {
        beforeTest {
            mockkStatic(::parseUri)
        }
        afterTest {
            unmockkStatic(::parseUri)
        }

        context("test with mocks") {
            testColumnAdapterWithMocks(
                adapter = UriColumnAdapter,
                fakeDatabaseValue = "test",
                serialize = { serialize() },
                parse = { parseUri(it) },
            )
        }
    },
)
