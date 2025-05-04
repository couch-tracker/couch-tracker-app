package io.github.couchtracker.utils

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.LocalDateTime

class TimeTest : FunSpec(
    {
        test("Rounding rounds") {
            LocalDateTime(2020, 2, 14, 18, 25, 44, 789).roundToSeconds() shouldBe
                LocalDateTime(2020, 2, 14, 18, 25, 44)
        }
    },
)
