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
    val language: TmdbLanguage,
) {
    suspend fun details(cache: TmdbCache): TmdbShowDetail = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-details",
        get = { cache.showDetailsCacheQueries.get(id, language, ::TmdbTimestampedEntry) },
        put = { cache.showDetailsCacheQueries.put(tmdbId = id, language = language, details = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.show.getDetails(
                showId = id.value,
                language = language.apiParameter,
            )
        },
    )

    suspend fun images(cache: TmdbCache): TmdbImages = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-images",
        get = { cache.showImagesCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.showImagesCacheQueries.put(tmdbId = id, images = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.show.getDetails(
                showId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.IMAGES),
            ).images ?: error("images cannot be null")
        },
    )

    suspend fun aggregateCredits(cache: TmdbCache): TmdbAggregateCredits = tmdbGetOrDownload(
        entryTag = "${id.toExternalId().serialize()}-$language-credits",
        get = { cache.showAggregateCreditsCacheQueries.get(id, ::TmdbTimestampedEntry) },
        put = { cache.showAggregateCreditsCacheQueries.put(tmdbId = id, credits = it.value, lastUpdate = it.lastUpdate) },
        downloader = {
            it.show.getDetails(
                showId = id.value,
                language = null,
                appendResponses = listOf(AppendResponse.AGGREGATE_CREDITS),
            ).aggregateCredits ?: error("aggregateCredits cannot be null")
        },
    )
}

fun ApiTmdbShow.toInternalTmdbShow(language: TmdbLanguage) = TmdbShow(TmdbShowId(id), language)
