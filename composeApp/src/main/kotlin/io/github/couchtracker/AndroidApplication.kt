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
import io.github.couchtracker.utils.currentLocalesFlow
import kotlinx.coroutines.MainScope
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.dsl.module

private const val IMAGE_CACHE_PERCENT = 0.5

class AndroidApplication : Application(), SingletonImageLoader.Factory {

    val currentLocaleFlow = currentLocalesFlow()

    override fun onCreate() {
        super.onCreate()

        val appModule = module {
            single { currentLocaleFlow }
        }

        startKoin {
            logger(AndroidLogger(Level.DEBUG))

            androidContext(this@AndroidApplication)

            modules(
                appModule,
                AppDataModule,
                ProfileDbModule,
                TmdbCacheDbModule,
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

    companion object {
        var scope = MainScope()
    }
}
