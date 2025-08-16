package io.github.couchtracker.tmdb

import app.moviebase.tmdb.Tmdb3
import io.github.couchtracker.utils.lazyEagerModule

val TmdbModule = lazyEagerModule {
    single {
        Tmdb3 {
            tmdbApiKey = TmdbConfig.API_KEY
            useTimeout = true
            maxRequestRetries = 3
        }
    }
    single {
        TmdbMemoryCache()
    }
}
