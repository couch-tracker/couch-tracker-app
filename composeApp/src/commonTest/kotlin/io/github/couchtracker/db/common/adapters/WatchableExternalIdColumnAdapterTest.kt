package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.user.WatchableExternalId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.verify

class WatchableExternalIdColumnAdapterTest : FunSpec(
    {
        test("encode()") {
            val externalId = mockk<WatchableExternalId> {
                every { serialize() } returns "mocked"
            }
            WatchableExternalIdColumnAdapter.encode(externalId) shouldBe "mocked"
            verify(exactly = 1) { externalId.serialize() }
        }
        test("decode()") {
            val externalId = mockk<WatchableExternalId>()
            mockkObject(WatchableExternalId.Companion) {
                every { WatchableExternalId.parse(any()) } returns externalId
                WatchableExternalIdColumnAdapter.decode("something") shouldBe externalId
                verify(exactly = 1) { WatchableExternalId.parse("something") }
            }
        }
    },
)
