package io.github.couchtracker.db.user

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class UserDbUtilsTest : FunSpec(
    {
        val context = mockk<Context> {
            every { getDatabasePath(any()) } returns File("/some/path")
        }
        test("getCachedDbNameForUser()") {
            val dbPath = UserDbUtils.getCachedDbNameForUser(context, 123)
            dbPath.name shouldBe "123.cached.db"
        }
        test("getManagedDbNameForUser()") {
            val dbPath = UserDbUtils.getManagedDbNameForUser(context, 987)
            dbPath.name shouldBe "987.db"
        }
    },
)
