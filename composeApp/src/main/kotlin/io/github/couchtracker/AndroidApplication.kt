package io.github.couchtracker

import android.app.Application
import io.github.couchtracker.db.app.AppDataModule
import io.github.couchtracker.db.profile.ProfileDbModule
import io.github.couchtracker.db.tmdbCache.TmdbCacheDbModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.logger.AndroidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AndroidApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            logger(AndroidLogger(Level.DEBUG))

            androidContext(this@AndroidApplication)

            modules(
                AppDataModule,
                ProfileDbModule,
                TmdbCacheDbModule,
            )
        }
    }
}
