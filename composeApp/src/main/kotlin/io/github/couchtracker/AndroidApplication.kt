package io.github.couchtracker

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.memory.MemoryCache
import coil3.request.crossfade
import coil3.size.Precision
import io.github.couchtracker.db.app.AppDataModule
import io.github.couchtracker.db.profile.ProfileDbModule
import io.github.couchtracker.db.tmdbCache.TmdbCacheDbModule
import io.github.couchtracker.tmdb.TmdbModule
import io.github.couchtracker.utils.LocaleModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.lazyModules
import org.koin.core.logger.Level

private const val IMAGE_CACHE_PERCENT = 0.5

class AndroidApplication : Application(), SingletonImageLoader.Factory {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            logger(AndroidLogger(Level.DEBUG))

            androidContext(this@AndroidApplication)

            lazyModules(
                AppDataModule,
                LocaleModule,
                ProfileDbModule,
                TmdbCacheDbModule,
                TmdbModule,
            )
        }
    }

    override fun newImageLoader(context: Context): ImageLoader {
        // This can be called multiple times, but only one of the results will be actually used.
        // See coil3.SingletonImageLoader
        return ImageLoader.Builder(context)
            .precision(Precision.INEXACT)
            .crossfade(true)
            .memoryCache {
                MemoryCache.Builder().maxSizePercent(this, IMAGE_CACHE_PERCENT).build()
            }
            .build()
    }
}
