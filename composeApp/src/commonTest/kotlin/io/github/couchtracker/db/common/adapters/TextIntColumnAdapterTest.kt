package io.github.couchtracker.db.common.adapters

import io.kotest.core.spec.style.FunSpec

class TextIntColumnAdapterTest : FunSpec(
    {
        context("TextIntColumnAdapter") {
            testColumnAdapter(
                columnAdapter = TextIntColumnAdapter,
                valid = listOf(
                    ColumnAdapterTest(
                        databaseValues = listOf("0"),
                        decodedValue = 0,
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf("1"),
                        decodedValue = 1,
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf("123456789"),
                        decodedValue = 123_456_789,
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf("2147483647"),
                        decodedValue = 2_147_483_647,
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf("-123"),
                        decodedValue = -123,
                    ),
                ),
                invalid = listOf(
                    "",
                    "  10",
                    "1   ",
                    "1324e10",
                    "1F",
                    // outside of Int boundary
                    "2147483648",
                    "-2147483649",
                ),
            )
        }
    },
)
