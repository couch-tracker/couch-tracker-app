package io.github.couchtracker.db.profile.externalids

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.scopes.FunSpecRootScope
import io.kotest.datatest.withTests
import io.kotest.matchers.shouldBe

fun <EID : ExternalId> FunSpecRootScope.testParseAndSerialize(
    type: ExternalId.SealedInterfacesCompanion<EID>,
    validTestCases: List<ExternalIdParseSerializeTest<EID>>,
    invalidValues: List<String>,
) {
    context("parse") {
        withTests(
            nameFn = { it.first },
            ts = validTestCases.flatMap { tc -> tc.values.map { it to tc.id } },
        ) { (value, expected) ->
            type.parse(value) shouldBe expected
        }
        context("fails with invalid IDs") {
            withTests(nameFn = { it.ifEmpty { "<empty>" }.ifBlank { "<blank>" } }, invalidValues) { id ->
                shouldThrow<IllegalArgumentException> {
                    type.parse(id)
                }
            }
        }
    }

    context("serialize") {
        withTests(nameFn = { it.id.toString() }, validTestCases) { (id, expected) ->
            type.serialize(id) shouldBe expected.first()
        }
    }
}

data class ExternalIdParseSerializeTest<out EID : ExternalId>(
    val id: EID,
    val values: List<String>,
)
