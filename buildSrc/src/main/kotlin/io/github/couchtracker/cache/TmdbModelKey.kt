package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn

internal enum class TmdbModelKey(val column: SqlColumn) {

    MOVIE_ID(SqlColumn.int("tmdbId", "io.github.couchtracker.tmdb.TmdbMovieId")),
    SHOW_ID(SqlColumn.int("tmdbId", "io.github.couchtracker.tmdb.TmdbShowId")),
    SEASON_ID(SqlColumn.text("tmdbId", "io.github.couchtracker.tmdb.TmdbSeasonId")),

}
