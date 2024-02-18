package io.github.couchtracker.db.user

import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class TmdbTest : FunSpec(
    {
        context("requireTmdbId()") {
            context("works with valid IDs") {
                withData(listOf(1, 50, 1200, 999_999, Long.MAX_VALUE)) {id ->
                    assertDoesNotThrow {
                        requireTmdbId(id)
                    }
                }
            }
            context("fails with invalid IDs") {
                withData(0, -1, -1234, Long.MIN_VALUE) {id ->
                    assertThrows<IllegalArgumentException> {
                        requireTmdbId(id)
                    }
                }
            }
        }
    },
)
