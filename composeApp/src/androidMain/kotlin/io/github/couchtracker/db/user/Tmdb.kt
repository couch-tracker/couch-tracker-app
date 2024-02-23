package io.github.couchtracker.db.user

/**
 * Throws [IllegalArgumentException] when the given [id] is not a valid TMDB ID.
 */
fun requireTmdbId(id: Long) {
    require(id > 0) { "Invalid non-positive TMDB id: $id" }
}
