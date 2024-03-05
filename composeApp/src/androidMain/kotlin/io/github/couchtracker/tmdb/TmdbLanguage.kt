package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter

/**
 * A language, compatible with TMDB APIs.
 * Specification is on https://developer.themoviedb.org/docs/languages
 *
 * @property language ISO 639-1
 * @property country ISO 3166-1
 */
data class TmdbLanguage(
    val language: String,
    val country: String?,
) {
    init {
        require(language.length == 2) { "Invalid language '$language'" }
        require(language.all { it in 'a'..'z' }) { "Invalid language '$language'" }
        if (country != null) {
            require(country.length == 2) { "Invalid country '$country'" }
            require(country.all { it in 'A'..'Z' }) { "Invalid country '$country'" }
        }
    }

    val apiParameter: String
        get() = if (country != null) {
            "$language-$country"
        } else {
            language
        }

    fun serialize() = apiParameter

    companion object {
        val English = TmdbLanguage("en", null)

        val dbAdapter = object : ColumnAdapter<TmdbLanguage, String> {
            override fun decode(databaseValue: String) = parse(databaseValue)
            override fun encode(value: TmdbLanguage) = value.serialize()
        }

        /**
         * Parses a TmdbLanguage. Supported formats are either `language-COUNTRY` or `language`, where
         * `language` uses ISO 639-1, and `country` uses ISO 3166-1.
         */
        @Suppress("MagicNumber")
        fun parse(serializedValue: String): TmdbLanguage {
            val tokens = serializedValue.split('-', limit = 2)
            return when (tokens.size) {
                1 -> TmdbLanguage(serializedValue, null)
                2 -> TmdbLanguage(tokens[0], tokens[1])
                else -> throw IllegalArgumentException("Invalid tmdb language '$serializedValue'")
            }
        }
    }
}
