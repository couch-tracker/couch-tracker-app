package io.github.couchtracker.db.common

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class DbPathTest : FunSpec(
    {
        test("appDatabase() works") {
            val dbFile = File("/some/path/somename.db")
            val context = mockk<Context> {
                every { getDatabasePath("somename") } returns dbFile
            }
            val path = DbPath.appDatabase(context, "somename")
            path.name shouldBe "somename"
            path.file shouldBe dbFile
        }
        test("appCache() works") {
            val appCache = File("/some/path")
            val context = mockk<Context> {
                every { cacheDir } returns appCache
            }
            val path = DbPath.appCache(context, "somename")
            path.name shouldBe "somename"
            path.file shouldBe File(appCache, "somename")
        }
    },
)
