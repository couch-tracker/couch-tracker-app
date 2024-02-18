package io.github.couchtracker.db.user

fun requireTmdbId(id: Long) {
    require(id > 0) { "Invalid non-positive TMDB id: $id" }
}
