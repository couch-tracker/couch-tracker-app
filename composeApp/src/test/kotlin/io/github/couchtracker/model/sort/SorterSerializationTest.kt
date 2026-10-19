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
            Json.decodeFromString<Sorter>("{\"type\":\"Random\",\"seed\":123}") shouldEqual Sorter.Random(123)
            Json.decodeFromString<Sorter>("{\"type\":\"ByRuntime\"}") shouldEqual Sorter.ByRuntime(true)
            Json.decodeFromString<Sorter>("{\"type\":\"ByRuntime\",\"asc\":false}") shouldEqual Sorter.ByRuntime(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByRuntime\",\"asc\":true}") shouldEqual Sorter.ByRuntime(true)
            Json.decodeFromString<Sorter>("{\"type\":\"ByEpisodeRuntime\"}") shouldEqual Sorter.ByEpisodeRuntime(true)
            Json.decodeFromString<Sorter>("{\"type\":\"ByEpisodeRuntime\",\"asc\":false}") shouldEqual Sorter.ByEpisodeRuntime(false)
            Json.decodeFromString<Sorter>("{\"type\":\"ByEpisodeRuntime\",\"asc\":true}") shouldEqual Sorter.ByEpisodeRuntime(true)
        }
        test("all sorters can be serialized") {
            Json.encodeToString<Sorter>(Sorter.Alphabetical()) shouldEqual "{\"type\":\"Alphabetical\"}"
            Json.encodeToString<Sorter>(Sorter.ByTmdbRating()) shouldEqual "{\"type\":\"ByTmdbRating\"}"
            Json.encodeToString<Sorter>(Sorter.ByReleaseDate()) shouldEqual "{\"type\":\"ByReleaseDate\"}"
            Json.encodeToString<Sorter>(Sorter.Random(123)) shouldEqual "{\"type\":\"Random\",\"seed\":123}"
            Json.encodeToString<Sorter>(Sorter.ByRuntime()) shouldEqual "{\"type\":\"ByRuntime\"}"
            Json.encodeToString<Sorter>(Sorter.ByEpisodeRuntime()) shouldEqual "{\"type\":\"ByEpisodeRuntime\"}"
        }
    },
)
