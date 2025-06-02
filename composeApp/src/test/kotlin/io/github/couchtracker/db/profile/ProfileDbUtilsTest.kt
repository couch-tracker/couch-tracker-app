package io.github.couchtracker.db.profile

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class ProfileDbUtilsTest : FunSpec(
    {
        val context = mockk<Context> {
            every { getDatabasePath(any()) } returns File("/some/path")
            every { cacheDir } returns File("/some/path")
        }
        test("getCachedDbNameForProfile()") {
            val dbPath = ProfileDbUtils.getCachedDbNameForProfile(context, 123)
            dbPath.name shouldBe "123.cached.db"
        }
        test("getManagedDbNameForProfile()") {
            val dbPath = ProfileDbUtils.getManagedDbNameForProfile(context, 987)
            dbPath.name shouldBe "987.db"
        }
    },
)
