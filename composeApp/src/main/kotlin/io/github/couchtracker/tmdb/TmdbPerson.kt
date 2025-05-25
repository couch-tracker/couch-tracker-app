package io.github.couchtracker.tmdb

/**
 * Class that represents a TMDB person.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbPerson(
    val id: TmdbPersonId,
    val language: TmdbLanguage,
)
