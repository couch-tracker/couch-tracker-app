package io.github.couchtracker.db

import androidx.documentfile.provider.DocumentFile
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Instant

class DocumentFileUtilsTest : FunSpec(
    {
        context("lastModifiedInstant()") {
            withData(
                0L to null,
                -1000L to Instant.parse("1969-12-31T23:59:59Z"),
                1000L to Instant.parse("1970-01-01T00:00:01Z"),
            ) { (milliseconds, expected) ->
                val documentFile = mockk<DocumentFile> {
                    every { lastModified() } returns milliseconds
                }
                documentFile.lastModifiedInstant() shouldBe expected
            }
        }
    },
)
