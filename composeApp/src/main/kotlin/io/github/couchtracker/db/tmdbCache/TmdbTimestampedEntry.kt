package io.github.couchtracker.db.tmdbCache

import kotlin.time.Instant

data class TmdbTimestampedEntry<T>(
    val value: T,
    val lastUpdate: Instant,
)
