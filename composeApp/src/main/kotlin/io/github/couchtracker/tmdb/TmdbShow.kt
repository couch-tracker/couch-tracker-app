package io.github.couchtracker.tmdb

import app.moviebase.tmdb.model.AppendResponse
import app.moviebase.tmdb.model.TmdbAggregateCredits
import app.moviebase.tmdb.model.TmdbImages
import app.moviebase.tmdb.model.TmdbShowDetail
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import app.moviebase.tmdb.model.TmdbShow as ApiTmdbShow

/**
 * Class that represents a TMDB show.
 * It holds no data, but provides the means to get all information,
 * either by downloading them or loading them from cache.
 */
data class TmdbShow(
    val id: TmdbShowId,
    val languages: TmdbLanguages,
) {
    suspend fun details(cache: TmdbCache): TmdbShowDetail = tmdbGetOrDownload(
        languages = languages,
        tryNextLanguage = { it.overview.isBlank() && it.tagline.isBlank() },
        entryTag = { lang -> "${id.toExternalId().serialize()}-$lang-details" },
        get = { lang -> cache.showDetailsCacheQueries.get(tmdbId = id, language = lang, mapper = ::TmdbTimestampedEntry) },
        put = { cache.showDetailsCacheQueries.put(tmdbId = id, language = it.language, details = it.value, lastUpdate = it.lastUpdate) },
        downloader = { lang -> show.getDetails(showId = id.value, language = lang.apiParameter) },
    )

    suspend fun images(cache: TmdbCache): TmdbImages = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-images",
        get = { cache.showImagesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.showImagesCacheQueries.put(tmdbId = id, images = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            show.getDetails(
                showId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.IMAGES),
            ).images ?: error("images cannot be null")
        },
    )

    suspend fun aggregateCredits(cache: TmdbCache): TmdbAggregateCredits = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-credits",
        get = { cache.showAggregateCreditsCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.showAggregateCreditsCacheQueries.put(tmdbId = id, credits = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            show.getDetails(
                showId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.AGGREGATE_CREDITS),
            ).aggregateCredits ?: error("aggregateCredits cannot be null")
        },
    )
}

val ApiTmdbShow.tmdbShowId get() = TmdbShowId(id)
