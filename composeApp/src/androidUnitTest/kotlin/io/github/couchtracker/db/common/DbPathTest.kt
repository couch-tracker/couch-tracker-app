package io.github.couchtracker.db.common

import android.content.Context
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.io.File

class DbPathTest : FunSpec(
    {
        test("of() works") {
            val file = File("/some/path")
            val context = mockk<Context> {
                every { getDatabasePath(any()) } returns file
            }
            val path = DbPath.of(context, "somename")
            path.name shouldBe "somename"
            path.file shouldBe file
        }
    },
)
