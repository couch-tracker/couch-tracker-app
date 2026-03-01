package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn

private const val TMDB_ID_COLUMN_NAME = "tmdbId"

internal interface CacheKey {
    val column: SqlColumn
}

internal enum class TmdbModelKey(override val column: SqlColumn) : CacheKey {
    MOVIE_ID(SqlColumn.int(TMDB_ID_COLUMN_NAME, "io.github.couchtracker.tmdb.TmdbMovieId")),
    SHOW_ID(SqlColumn.int(TMDB_ID_COLUMN_NAME, "io.github.couchtracker.tmdb.TmdbShowId")),
    SEASON_ID(SqlColumn.text(TMDB_ID_COLUMN_NAME, "io.github.couchtracker.tmdb.TmdbSeasonId")),
    EPISODE_ID(SqlColumn.text(TMDB_ID_COLUMN_NAME, "io.github.couchtracker.tmdb.TmdbEpisodeId")),
}

internal enum class LanguageKey(override val column: SqlColumn) : CacheKey {
    LANGUAGE(SqlColumn.text("language", "io.github.couchtracker.tmdb.TmdbLanguage")),
    LANGUAGES_FILTER(SqlColumn.text("languages", "io.github.couchtracker.tmdb.TmdbLanguagesFilter")),
}
