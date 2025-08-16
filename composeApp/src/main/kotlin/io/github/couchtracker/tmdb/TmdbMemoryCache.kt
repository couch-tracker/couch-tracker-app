package io.github.couchtracker.tmdb

import androidx.collection.LruCache

class TmdbMemoryCache {
    private val movieCache = LruCache<TmdbMovie, BaseTmdbMovie>(32)
    private val showCache = LruCache<TmdbShow, BaseTmdbShow>(32)

    fun registerItem(movie: BaseTmdbMovie) {
        movieCache.put(movie.id, movie)
    }

    fun registerItem(show: BaseTmdbShow) {
        showCache.put(show.id, show)
    }

    fun getMovie(id: TmdbMovie): BaseTmdbMovie? = movieCache[id]

    fun getShow(id: TmdbShow): BaseTmdbShow? = showCache[id]
}
