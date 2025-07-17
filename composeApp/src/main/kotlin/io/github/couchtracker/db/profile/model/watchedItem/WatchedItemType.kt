package io.github.couchtracker.db.profile.model.watchedItem

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Something that can be watched.
 */
enum class WatchedItemType(val fallbackRuntime: Duration) {
    MOVIE(2.hours),
    EPISODE(30.minutes),
}
