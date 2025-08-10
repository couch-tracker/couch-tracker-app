package io.github.couchtracker.db.tmdbCache

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.jsonAdapter
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMovieId
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.utils.lazyEagerModule
import org.koin.core.qualifier.named
import org.koin.dsl.bind

val TmdbCacheDbModule = lazyEagerModule {
    factory(named("TmdbCacheDb")) {
        AndroidSqliteDriverFactory(schema = TmdbCache.Schema)
    }.bind<SqliteDriverFactory>()

    single(createdAtStart = true) {
        val driverFactory = get<SqliteDriverFactory>(named("TmdbCacheDb"))
        TmdbCache(
            driver = driverFactory.getDriver(DbPath.appCache(get(), "tmdb-cache.db")),
            MovieDetailsCacheAdapter = MovieDetailsCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                languageAdapter = TmdbLanguage.COLUMN_ADAPTER,
                detailsAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            MovieReleaseDatesCacheAdapter = MovieReleaseDatesCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                releaseDatesAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            MovieCreditsCacheAdapter = MovieCreditsCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                creditsAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            MovieImagesCacheAdapter = MovieImagesCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                imagesAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            MovieVideosCacheAdapter = MovieVideosCache.Adapter(
                tmdbIdAdapter = TmdbMovieId.COLUMN_ADAPTER,
                videosAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            ShowDetailsCacheAdapter = ShowDetailsCache.Adapter(
                tmdbIdAdapter = TmdbShowId.COLUMN_ADAPTER,
                languageAdapter = TmdbLanguage.COLUMN_ADAPTER,
                detailsAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            ShowImagesCacheAdapter = ShowImagesCache.Adapter(
                tmdbIdAdapter = TmdbShowId.COLUMN_ADAPTER,
                imagesAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
            ShowAggregateCreditsCacheAdapter = ShowAggregateCreditsCache.Adapter(
                tmdbIdAdapter = TmdbShowId.COLUMN_ADAPTER,
                creditsAdapter = jsonAdapter(),
                lastUpdateAdapter = InstantColumnAdapter,
            ),
        )
    }
}
