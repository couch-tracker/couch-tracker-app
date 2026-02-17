package io.github.couchtracker.tmdb

import io.github.couchtracker.db.common.adapters.ColumnAdapterTest
import io.github.couchtracker.db.common.adapters.testColumnAdapter
import io.kotest.core.spec.style.FunSpec
import io.kotest.datatest.withData
import io.kotest.matchers.shouldBe

class TmdbLanguagesFilterTest : FunSpec(
    {
        context("TmdbLanguagesFilter") {
            context("COLUMN_ADAPTER") {
                testColumnAdapter(
                    columnAdapter = TmdbLanguagesFilter.COLUMN_ADAPTER,
                    valid = listOf(
                        ColumnAdapterTest(
                            databaseValues = listOf(""),
                            decodedValue = TmdbLanguagesFilter(languages = emptySet()),
                        ),
                        ColumnAdapterTest(
                            databaseValues = listOf("en-US"),
                            decodedValue = TmdbLanguagesFilter(
                                languages = setOf(
                                    TmdbLanguage("en", "US"),
                                ),
                            ),
                        ),
                        ColumnAdapterTest(
                            databaseValues = listOf(
                                "en,en-US,it-IT",
                                "en,en-US,,,it-IT,,",
                                "en-US,en,it-IT",
                                "en,it-IT,en-US",
                            ),
                            decodedValue = TmdbLanguagesFilter(
                                languages = setOf(
                                    TmdbLanguage("en", "US"),
                                    TmdbLanguage("en", null),
                                    TmdbLanguage("it", "IT"),
                                ),
                            ),
                        ),
                    ),
                    invalid = listOf(
                        "EN",
                        "EN,en",
                        "en-us",
                        "en-USA",
                        "en-US-US",
                        "--",
                        "asd",
                    ),
                )
            }
            context("apiParameter") {
                data class TestCase(
                    val languages: Set<String>,
                    val includeAll: String?,
                    val includeDifferentCountries: String?,
                    val includeItemsWithoutLanguage: String?,
                    val includeNone: String?,
                )
                withData(
                    ts = listOf(
                        TestCase(
                            languages = emptySet(),
                            includeAll = null,
                            includeDifferentCountries = null,
                            includeItemsWithoutLanguage = null,
                            includeNone = null,
                        ),
                        TestCase(
                            languages = setOf("en"),
                            includeAll = "en,null",
                            includeDifferentCountries = "en",
                            includeItemsWithoutLanguage = "en,null",
                            includeNone = "en",
                        ),
                        TestCase(
                            languages = setOf("en", "en-US"),
                            includeAll = "en,en-US,null",
                            includeDifferentCountries = "en,en-US",
                            includeItemsWithoutLanguage = "en,en-US,null",
                            includeNone = "en,en-US",
                        ),
                        TestCase(
                            languages = setOf("en-US"),
                            includeAll = "en-US,en,null",
                            includeDifferentCountries = "en-US,en",
                            includeItemsWithoutLanguage = "en-US,null",
                            includeNone = "en-US",
                        ),
                        TestCase(
                            languages = setOf("en-US", "it-IT"),
                            includeAll = "en-US,en,it-IT,it,null",
                            includeDifferentCountries = "en-US,en,it-IT,it",
                            includeItemsWithoutLanguage = "en-US,it-IT,null",
                            includeNone = "en-US,it-IT",
                        ),
                    ),
                ) { testCase ->
                    val filter = TmdbLanguagesFilter(
                        languages = testCase.languages.map { TmdbLanguage.parse(it) }.toSet(),
                    )
                    filter.apiParameter(
                        includeItemsOfDifferentCountries = true,
                        includeItemsWithoutLanguage = true,
                    ) shouldBe testCase.includeAll
                    filter.apiParameter(
                        includeItemsOfDifferentCountries = true,
                        includeItemsWithoutLanguage = false,
                    ) shouldBe testCase.includeDifferentCountries
                    filter.apiParameter(
                        includeItemsOfDifferentCountries = false,
                        includeItemsWithoutLanguage = true,
                    ) shouldBe testCase.includeItemsWithoutLanguage
                    filter.apiParameter(
                        includeItemsOfDifferentCountries = false,
                        includeItemsWithoutLanguage = false,
                    ) shouldBe testCase.includeNone
                }
            }
        }
    },
)
