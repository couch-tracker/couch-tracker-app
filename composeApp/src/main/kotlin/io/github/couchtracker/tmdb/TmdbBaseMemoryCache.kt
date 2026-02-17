package io.github.couchtracker.tmdb

import androidx.collection.LruCache

class TmdbBaseMemoryCache {
    private val movieCache = LruCache<TmdbBaseCacheKey<TmdbMovieId, TmdbLanguage>, BaseTmdbMovie>(32)
    private val showCache = LruCache<TmdbBaseCacheKey<TmdbShowId, TmdbLanguage>, BaseTmdbShow>(32)

    fun registerItem(movie: BaseTmdbMovie) {
        movieCache.put(movie.key, movie)
    }

    fun registerItem(show: BaseTmdbShow) {
        showCache.put(show.key, show)
    }

    fun getMovie(movie: TmdbMovieId, language: TmdbLanguage): BaseTmdbMovie? = movieCache[TmdbBaseCacheKey(movie, language)]

    fun getShow(show: TmdbShowId, language: TmdbLanguage): BaseTmdbShow? = showCache[TmdbBaseCacheKey(show, language)]
}

data class TmdbBaseCacheKey<ID : TmdbId, L>(
    val id: ID,
    val language: L,
)
