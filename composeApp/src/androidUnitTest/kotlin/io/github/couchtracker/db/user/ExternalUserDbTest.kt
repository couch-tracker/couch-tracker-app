package io.github.couchtracker.db.user

import io.github.couchtracker.db.app.User
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.mockk.every
import io.mockk.mockk

class ExternalUserDbTest : FunSpec(
    {
        context("of()") {
            test("fails if User doesn't have external uri") {
                val user = mockk<User> {
                    every { externalFileUri } returns null
                }
                shouldThrow<IllegalArgumentException> {
                    ExternalUserDb.of(user = user)
                }
            }
        }
    },
)
