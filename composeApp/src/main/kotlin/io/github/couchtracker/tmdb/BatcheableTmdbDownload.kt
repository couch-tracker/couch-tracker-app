package io.github.couchtracker.tmdb

import app.cash.sqldelight.Query
import app.cash.sqldelight.db.QueryResult
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.db.tmdbCache.TmdbTimestampedEntry
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.api.ApiException
import io.github.couchtracker.utils.api.ApiResult
import io.github.couchtracker.utils.api.BatchDownloader
import kotlinx.coroutines.flow.Flow
import kotlin.reflect.KProperty1
import kotlin.time.Duration
import kotlin.time.Instant

typealias LocalizedQueryBuilder<ID, T> = (ID, TmdbLanguage, (T, Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>
typealias QueryBuilder<ID, T> = (ID, (details: T, lastUpdate: Instant) -> TmdbTimestampedEntry<T>) -> Query<TmdbTimestampedEntry<T>>

class BatchDownloadableFlowBuilder<ID, Req, Res>(
    val id: ID,
    val logTag: String,
    val prepareRequest: (Req) -> Req,
) {

    fun <T : Any> extractFromResponse(extractFromResponse: (Res) -> T): Extract<T> {
        return Extract { response ->
            Result.Value(extractFromResponse(response))
        }
    }

    fun <T : Any> extractNonNullFromResponse(property: KProperty1<Res, T?>): Extract<T> {
        return extractNonNullFromResponse(fieldName = property.name, extractFromResponse = property::invoke)
    }

    fun <T : Any> extractNonNullFromResponse(fieldName: String, extractFromResponse: (Res) -> T?): Extract<T> {
        return Extract { response ->
            val result = extractFromResponse(response)
            if (result == null) {
                Result.Error(ApiException.DeserializationError("Unexpected null field $fieldName", cause = null))
            } else {
                Result.Value(result)
            }
        }
    }

    inner class Extract<T : Any>(
        val extractFromResponse: (Res) -> ApiResult<T>,
    ) {
        fun localized(
            language: TmdbLanguage,
            loadFromCacheFn: (TmdbCache) -> LocalizedQueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, TmdbLanguage, T, Instant) -> QueryResult<Long>,
        ) = Cached(
            logTag = "$language-$logTag",
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, language, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, language, data.value, data.lastUpdate) },
        )

        fun notLocalized(
            loadFromCacheFn: (TmdbCache) -> QueryBuilder<ID, T>,
            putInCacheFn: (TmdbCache) -> (ID, T, Instant) -> QueryResult<Long>,
        ) = Cached(
            logTag = logTag,
            loadFromCache = { cache -> loadFromCacheFn(cache)(id, ::TmdbTimestampedEntry) },
            putInCache = { cache, data -> putInCacheFn(cache)(id, data.value, data.lastUpdate) },
        )

        inner class Cached(
            val logTag: String,
            val loadFromCache: (cache: TmdbCache) -> Query<TmdbTimestampedEntry<T>>,
            val putInCache: (cache: TmdbCache, TmdbTimestampedEntry<T>) -> Unit,
        ) {
            fun flow(
                downloader: BatchDownloader<Req, Res>,
                expiration: Duration = TMDB_CACHE_EXPIRATION_DEFAULT,
            ): Flow<ApiResult<T>> {
                return tmdbGetOrDownload(
                    entryTag = logTag,
                    loadFromCache = loadFromCache,
                    putInCache = putInCache,
                    downloader = { downloader.download(prepareRequest, extractFromResponse) },
                    expiration = expiration,
                )
            }
        }
    }
}
