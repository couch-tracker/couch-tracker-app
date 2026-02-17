package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter

/**
 * A class that represents a set of languages, compatible with TMDB APIs that support a language filter.
 */
@JvmInline
value class TmdbLanguagesFilter(
    val languages: Set<TmdbLanguage>,
) {

    fun apiParameter(
        includeItemsOfDifferentCountries: Boolean = true,
        includeItemsWithoutLanguage: Boolean = true,
    ): String? {
        return if (languages.isEmpty()) {
            null
        } else {
            buildSet {
                for (language in languages) {
                    add(language.languageTag)
                    if (includeItemsOfDifferentCountries && language.country != null) {
                        add(language.language)
                    }
                }
                if (includeItemsWithoutLanguage) {
                    add("null")
                }
            }.joinToString(",")
        }
    }

    companion object {

        val COLUMN_ADAPTER = object : ColumnAdapter<TmdbLanguagesFilter, String> {
            override fun decode(databaseValue: String) = TmdbLanguagesFilter(
                languages = databaseValue
                    .split(",")
                    .filter { it.isNotEmpty() }
                    .map { TmdbLanguage.parse(it) }
                    .toSet(),
            )

            override fun encode(value: TmdbLanguagesFilter) = value.languages
                .map { it.serialize() }
                .sorted()
                .joinToString(",")
        }
    }
}
