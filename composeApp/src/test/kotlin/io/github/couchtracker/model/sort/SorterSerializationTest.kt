package io.github.couchtracker.model.sort

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.equals.shouldEqual
import kotlinx.serialization.json.Json

class SorterSerializationTest : FunSpec(
    {
        test("deserialization is stable") {
            Json.decodeFromString<Sorter>("{\"type\":\"Alphabetical\"}") shouldEqual Sorter.Alphabetical(true)
            Json.decodeFromString<Sorter>("{\"type\":\"Alphabetical\",\"asc\":true}") shouldEqual Sorter.Alphabetical(true)
            Json.decodeFromString<Sorter>("{\"type\":\"Alphabetical\",\"asc\":false}") shouldEqual Sorter.Alphabetical(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByTmdbRating\"}") shouldEqual Sorter.ByTmdbRating(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByTmdbRating\",\"asc\":false}") shouldEqual Sorter.ByTmdbRating(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByTmdbRating\",\"asc\":true}") shouldEqual Sorter.ByTmdbRating(true)
            Json.decodeFromString<Sorter>("{\"type\":\"ByReleaseDate\"}") shouldEqual Sorter.ByReleaseDate(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByReleaseDate\",\"asc\":false}") shouldEqual Sorter.ByReleaseDate(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByReleaseDate\",\"asc\":true}") shouldEqual Sorter.ByReleaseDate(true)
        }
    },
)
