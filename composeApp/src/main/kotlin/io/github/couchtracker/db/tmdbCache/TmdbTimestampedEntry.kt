package io.github.couchtracker.db.tmdbCache

import kotlinx.datetime.Instant

data class TmdbTimestampedEntry<T>(
    val value: T,
    val lastUpdate: Instant,
)
