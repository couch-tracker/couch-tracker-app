package io.github.couchtracker.db.tmdbCache

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.couchtracker.db.common.jsonAdapter
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovieId
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

object TmdbCacheDb {
    fun get(context: Context): TmdbCache {
        return TmdbCache(
            driver = AndroidSqliteDriver(
                // See https://github.com/cashapp/sqldelight/issues/5058
                TmdbCache.Schema.synchronous(),
                context,
                "tmdb-cache.db",
                factory = RequerySQLiteOpenHelperFactory(),
            ),
            MovieDetailsCacheAdapter = MovieDetailsCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                languageAdapter = TmdbLanguage.COLUMN_ADAPTER,
                detailsAdapter = jsonAdapter(),
            ),
            MovieReleaseDatesCacheAdapter = MovieReleaseDatesCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                releaseDatesAdapter = jsonAdapter(),
            ),
        )
    }
}
