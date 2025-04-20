package io.github.couchtracker.db.tmdbCache

import app.cash.sqldelight.async.coroutines.synchronous
import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.common.adapters.jsonAdapter
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovieId
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val TmdbCacheDbModule = module {
    factory(named("TmdbCacheDb")) {
        AndroidSqliteDriverFactory(schema = TmdbCache.Schema.synchronous())
    }.bind<SqliteDriverFactory>()

    single {
        val driverFactory = get<SqliteDriverFactory>(named("TmdbCacheDb"))
        TmdbCache(
            driver = driverFactory.getDriver(DbPath.of(get(), "tmdb-cache.db")),
            MovieDetailsCacheAdapter = MovieDetailsCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                languageAdapter = TmdbLanguage.COLUMN_ADAPTER,
                detailsAdapter = jsonAdapter(),
            ),
            MovieReleaseDatesCacheAdapter = MovieReleaseDatesCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                releaseDatesAdapter = jsonAdapter(),
            ),
            MovieCreditsCacheAdapter = MovieCreditsCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                creditsAdapter = jsonAdapter(),
            ),
            MovieImagesCacheAdapter = MovieImagesCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                imagesAdapter = jsonAdapter(),
            ),
        )
    }
}
