package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.kotest.core.spec.style.FunSpec

class JsonListColumnAdapterTest : FunSpec(
    {
        val invalidJsonStringLists = listOf(
            "",
            "  ",
            "not valid json",
            """["10","12""", // incomplete (end)
            """"10","12"]""", // incomplete (start)
            """["1", false, "3"]""", // contains invalid elements
            """{}""", // empty object
            """{"hello": "world"}""", // object
        )
        val emptyJsonLists = listOf(
            """[]""",
            """[ ]""",
            """[     ]""",
            """  [     ]   """,
        )
        context("jsonList()") {
            testColumnAdapter(
                columnAdapter = HexColumnAdapter.jsonList(),
                valid = listOf(
                    ColumnAdapterTest(
                        databaseValues = emptyJsonLists,
                        decodedValue = emptyList(),
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf(
                            """["A","14","1E","28"]""",
                            """["A", "14",   "1E", "28"]""",
                        ),
                        decodedValue = listOf(10, 20, 30, 40),
                    ),
                ),
                invalid = invalidJsonStringLists,
            )
        }
        context("jsonSet()") {
            testColumnAdapter(
                columnAdapter = HexColumnAdapter.jsonSet(),
                valid = listOf(
                    ColumnAdapterTest(
                        databaseValues = emptyJsonLists,
                        decodedValue = emptySet(),
                    ),
                    ColumnAdapterTest(
                        databaseValues = listOf(
                            """["A","14","1E","28"]""",
                            """["A","A","14","1E","28","28"]""",
                            """["A","A",    "14","1E",   "28", "A",  "28", "A"]""",
                        ),
                        decodedValue = setOf(10, 20, 30, 40),
                    ),
                ),
                invalid = invalidJsonStringLists,
            )
        }
    },
)

private object HexColumnAdapter : ColumnAdapter<Int, String> {
    override fun decode(databaseValue: String) = databaseValue.toInt(radix = 16)

    override fun encode(value: Int) = value.toString(radix = 16).uppercase()
}
