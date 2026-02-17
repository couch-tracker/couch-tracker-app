package io.github.couchtracker.tmdb

/**
 * Class representing a list of [TmdbLanguages] to use to get the localized something of a TMDB item.
 *
 * Languages should be tried in order, and use the first available localized content.
 *
 * This class guarantees that the list is not empty, does not contain duplicates, and that does not exceed [TmdbLanguages.MAX_LANGUAGES].
 */
@JvmInline
value class TmdbLanguages(val languages: List<TmdbLanguage>) {

    init {
        require(languages.isNotEmpty()) { "TMDB language list must not be empty" }
        require(languages.size <= MAX_LANGUAGES) { "TMDB language list too big: ${languages.size}, max is $MAX_LANGUAGES" }
        require(languages.distinct().size == languages.size) { "TMDB language list contains duplicates: $languages" }
    }

    val size get() = languages.size

    /**
     * Language to use when calling an API that supports only a single language as input. The first language is returned.
     */
    val apiLanguage get() = languages.first()

    fun toTmdbLanguagesFilter() = TmdbLanguagesFilter(languages.toSet())

    fun tryPlus(another: TmdbLanguage) = TmdbLanguages((languages + another).distinct())

    fun tryMinus(another: TmdbLanguage) = if (size > 1) TmdbLanguages(languages - another) else this

    fun withMovedItem(fromIndex: Int, toIndex: Int): TmdbLanguages {
        require(fromIndex in languages.indices)
        require(toIndex in languages.indices)

        return TmdbLanguages(
            languages = languages.toMutableList().apply {
                add(index = toIndex, element = removeAt(fromIndex))
            },
        )
    }

    fun isMaxLanguages() = size == MAX_LANGUAGES

    fun serialize() = languages.joinToString(separator = ",") { it.serialize() }

    companion object {
        const val MAX_LANGUAGES = 5

        fun parse(value: String): TmdbLanguages {
            return TmdbLanguages(value.split(",").map { TmdbLanguage.parse(it) })
        }
    }
}
