package io.github.couchtracker.db.tmdbCache

import io.github.couchtracker.tmdb.TmdbLanguage
import kotlin.time.Instant

data class TmdbTimestampedEntry<T>(
    val value: T,
    val lastUpdate: Instant,
) {

    fun localized(language: TmdbLanguage) = TmdbLocalizedTimestampedEntry(
        value = value,
        language = language,
        lastUpdate = lastUpdate,
    )
}

data class TmdbLocalizedTimestampedEntry<T>(
    val value: T,
    val language: TmdbLanguage,
    val lastUpdate: Instant,
)
