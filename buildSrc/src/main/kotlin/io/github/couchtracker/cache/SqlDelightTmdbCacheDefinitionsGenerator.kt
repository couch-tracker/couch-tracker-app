package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File


/** Which caches to create */
private val CACHES = listOf(
    // Movie
    SqlCache.forLocalizedTmdbItem(
        name = "MovieDetailsCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbMovieDetail"),
    ),
    SqlCache.forTmdbItem(
        name = "MovieReleaseDatesCache",
        key = TmdbModelKey.MOVIE_ID,
        value = SqlColumn.text("releaseDates", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbReleaseDates>"),
    ),
    SqlCache.forLocalizedTmdbItem(
        name = "MovieCreditsCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbCredits"),
    ),
    SqlCache.forLocalizedTmdbItem(
        name = "MovieImagesCache",
        key = TmdbModelKey.MOVIE_ID,
        languageKey = LanguageKey.LANGUAGES_FILTER,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlCache.forTmdbItem(
        name = "MovieVideosCache",
        key = TmdbModelKey.MOVIE_ID,
        value = SqlColumn.text("videos", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbVideo>"),
    ),
    // Show
    SqlCache.forLocalizedTmdbItem(
        name = "ShowDetailsCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbShowDetail"),
    ),
    SqlCache.forLocalizedTmdbItem(
        name = "ShowImagesCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LanguageKey.LANGUAGES_FILTER,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    SqlCache.forLocalizedTmdbItem(
        name = "ShowAggregateCreditsCache",
        key = TmdbModelKey.SHOW_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("credits", "app.moviebase.tmdb.model.TmdbAggregateCredits"),
    ),
    // Season
    SqlCache.forLocalizedTmdbItem(
        name = "SeasonDetailsCache",
        key = TmdbModelKey.SEASON_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbSeasonDetail"),
    ),
    // Episode
    SqlCache.forLocalizedTmdbItem(
        name = "EpisodeDetailsCache",
        key = TmdbModelKey.EPISODE_ID,
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("details", "app.moviebase.tmdb.model.TmdbEpisodeDetail"),
    ),
    SqlCache.forLocalizedTmdbItem(
        name = "EpisodeImagesCache",
        key = TmdbModelKey.EPISODE_ID,
        languageKey = LanguageKey.LANGUAGES_FILTER,
        value = SqlColumn.text("images", "app.moviebase.tmdb.model.TmdbImages"),
    ),
    // Genres
    SqlCache.forLocalizedInformation(
        name = "MovieGenresCache",
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("genres", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbGenre>"),
    ),
    SqlCache.forLocalizedInformation(
        name = "TvGenresCache",
        languageKey = LanguageKey.LANGUAGE,
        value = SqlColumn.text("genres", "kotlin.collections.List<app.moviebase.tmdb.model.TmdbGenre>"),
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
