package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File


/** Which caches to create */
private val CACHES = listOf(
    // Movie
    SqlTmdbModelCache(
        name = "MovieDetailsCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LANGUAGE_COLUMN,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbMovieDetail"),
    ),
    SqlTmdbModelCache(
        name = "MovieReleaseDatesCache",
        key = TmdbModelKey.MOVIE_ID,
        value = SqlColumn.text("releaseDates", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbReleaseDates>"),
    ),
    SqlTmdbModelCache(
        name = "MovieCreditsCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LANGUAGE_COLUMN,
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbCredits"),
    ),
    SqlTmdbModelCache(
        name = "MovieImagesCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LANGUAGES_FILTER_COLUMN,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlTmdbModelCache(
        name = "MovieVideosCache",
        key = TmdbModelKey.MOVIE_ID,
        value = SqlColumn.text("videos", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbVideo>"),
    ),
    // Show
    SqlTmdbModelCache(
        name = "ShowDetailsCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LANGUAGE_COLUMN,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbShowDetail"),
    ),
    SqlTmdbModelCache(
        name = "ShowImagesCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LANGUAGES_FILTER_COLUMN,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlTmdbModelCache(
        name = "ShowAggregateCreditsCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LANGUAGE_COLUMN, //TODO?
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbAggregateCredits"),
    ),
    // Season
    SqlTmdbModelCache(
        name = "SeasonDetailsCache",
        key = TmdbModelKey.SEASON_ID,
        languageKey = LANGUAGE_COLUMN,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbSeasonDetail"),
    ),
    // Episode
    SqlTmdbModelCache(
        name = "EpisodeDetailsCache",
        key = TmdbModelKey.EPISODE_ID,
        languageKey = LANGUAGE_COLUMN,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbEpisodeDetail"),
    ),
    SqlTmdbModelCache(
        name = "EpisodeImagesCache",
        key = TmdbModelKey.EPISODE_ID,
        languageKey = LANGUAGES_FILTER_COLUMN,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
).also { cachesDefinitions ->
    check(cachesDefinitions.distinctBy { it.name }.size == cachesDefinitions.size) {
        "Duplicate cache names"
    }
}

/**
 * A Gradle Task to generate the SQLDelight definitions related to the Tmdb Cache.
 */
open class SqlDelightTmdbCacheDefinitionsGenerator : DefaultTask() {
    /** Where to generate the .sq files */
    private val sqldelightRoot =
        project.layout.buildDirectory.dir("generated/sqldelight/tmdbCache/io/github/couchtracker/db/tmdbCache").get().asFile

    init {
        group = "io.github.couchtracker"
        description = "Generates the SqlDelight definitions for local the Tmdb Cache"
        outputs.dir(sqldelightRoot)
    }

    @TaskAction
    fun run() {
        sqldelightRoot.deleteRecursively()
        sqldelightRoot.mkdirs()
        for (cache in CACHES) {
            val cacheFile = File(sqldelightRoot, "${cache.name}.sq")
            cacheFile.writeText(cache.toSql())
        }
    }
}
