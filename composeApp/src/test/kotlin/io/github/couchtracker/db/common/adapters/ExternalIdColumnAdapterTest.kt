package io.github.couchtracker.db.common.adapters

import io.github.couchtracker.db.profile.ExternalId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

class ExternalIdColumnAdapterTest : FunSpec(
    {
        test("encode()") {
            val adapter = ExternalIdColumnAdapter<ExternalId>(type = mockk())
            val externalId = mockk<ExternalId> {
                every { serialize() } returns "mocked"
            }
            adapter.encode(externalId) shouldBe "mocked"
            verify(exactly = 1) { externalId.serialize() }
        }
        test("decode()") {
            val externalId = mockk<ExternalId>()
            val type = mockk<ExternalId.SealedInterfacesCompanion<ExternalId>> {
                every { parse(any()) } returns externalId
            }
            val adapter = ExternalIdColumnAdapter(type = type)
            adapter.decode("something") shouldBe externalId
            verify(exactly = 1) { type.parse("something") }
        }
    },
)
