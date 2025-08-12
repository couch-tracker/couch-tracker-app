package io.github.couchtracker.tmdb

import androidx.collection.LruCache

class TmdbBaseMemoryCache {
    private val movieCache = LruCache<TmdbBaseCacheKey<TmdbMovieId>, BaseTmdbMovie>(32)
    private val showCache = LruCache<TmdbBaseCacheKey<TmdbShowId>, BaseTmdbShow>(32)

    fun registerItem(movie: BaseTmdbMovie) {
        movieCache.put(movie.key, movie)
    }

    fun registerItem(show: BaseTmdbShow) {
        showCache.put(show.key, show)
    }

    fun getMovie(movie: TmdbMovie): BaseTmdbMovie? = movieCache[movie.baseCacheKey]

    fun getShow(show: TmdbShow): BaseTmdbShow? = showCache[show.baseCacheKey]
}

data class TmdbBaseCacheKey<ID : TmdbId>(
    val id: ID,
    val language: TmdbLanguage,
)

val TmdbMovie.baseCacheKey get() = TmdbBaseCacheKey(id = id, language = languages.apiLanguage)
val TmdbShow.baseCacheKey get() = TmdbBaseCacheKey(id = id, language = languages.apiLanguage)
