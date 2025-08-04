package io.github.couchtracker.tmdb

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.Bcp47Language
import io.github.couchtracker.utils.MixedValueTree
import java.util.Locale

/**
 * A language, compatible with TMDB APIs.
 * Specification is on https://developer.themoviedb.org/docs/languages
 *
 * @property language ISO 639-1
 * @property country ISO 3166-1. Technically TMDB always has a country in its language, but it also supports passing stuff with no country.
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

    val languageTag: String
        get() = if (country != null) {
            "$language-$country"
        } else {
            language
        }

    val apiParameter get() = languageTag

    fun serialize() = languageTag

    fun toBcp47Language() = Bcp47Language.of(languageTag)

    override fun toString() = serialize()

    companion object {
        val ENGLISH = TmdbLanguage("en", null)

        /**
         * Language to use if all else fails
         */
        val FALLBACK = ENGLISH

        val COLUMN_ADAPTER = object : ColumnAdapter<TmdbLanguage, String> {
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

/**
 * Converts [this] locale to a [TmdbLanguage].
 *
 * If this locale is not representable in a [TmdbLanguage] (e.g. uses ISO 639-2 code), `null` is returned.
 *
 * The fact that this method returns, does not guarantee that the returned [TmdbLanguage] is one of TMDB's
 * [primary translations](https://developer.themoviedb.org/reference/configuration-primary-translations).
 */
fun Locale.toTmdbLanguage(): TmdbLanguage? {
    return TmdbLanguage(
        language = this.language.takeIf { it.length == 2 }?.lowercase() ?: return null,
        country = this.country.takeIf { it.length == 2 }?.uppercase(),
    )
}

fun List<TmdbLanguage>.languageTree(
    comparator: Comparator<in MixedValueTree.NonRoot<TmdbLanguage, TmdbLanguage>>,
): MixedValueTree.Root<Unit, TmdbLanguage, TmdbLanguage> {
    return MixedValueTree.Root(
        value = Unit,
        children = this
            .groupBy { it.language }
            .map { (language, tmdbLanguages) ->
                if (tmdbLanguages.size > 1) {
                    MixedValueTree.Intermediate(
                        value = TmdbLanguage(language = language, country = null),
                        children = tmdbLanguages.map { MixedValueTree.Leaf(it) },
                    )
                } else {
                    MixedValueTree.Leaf(value = tmdbLanguages.single())
                }
            }
            .sortedWith(comparator),
    )
}
