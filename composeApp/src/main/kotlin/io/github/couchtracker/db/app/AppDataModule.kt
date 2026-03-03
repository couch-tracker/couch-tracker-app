package io.github.couchtracker.db.app

import app.cash.sqldelight.coroutines.asFlow
import io.github.couchtracker.AppCoroutineScope
import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.URIColumnAdapter
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.utils.lazyEagerModule
import io.github.couchtracker.utils.settings.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import org.koin.core.qualifier.named
import org.koin.dsl.bind

val AppDataModule = lazyEagerModule {
    single(named("AppDb")) {
        AndroidSqliteDriverFactory(schema = AppData.Schema)
    }.bind<SqliteDriverFactory>()

    single {
        val driverFactory = get<SqliteDriverFactory>(named("AppDb"))
        AppData(
            driver = driverFactory.getDriver(DbPath.appDatabase(context = get(), "app.db")),
            ProfileAdapter = Profile.Adapter(
                externalFileUriAdapter = URIColumnAdapter,
                cachedDbLastModifiedAdapter = InstantColumnAdapter,
            ),
        )
    }

    single<Flow<ProfilesInfo>> {
        val profiles = get<AppData>().profileQueries.selectAll().asFlow().map { it.executeAsList() }
        val profileId = AppSettings.get { CurrentProfileId }.map { it.current }
        profilesInfoFlow(profiles, profileId)
            .shareIn(get<AppCoroutineScope>(), SharingStarted.Eagerly, 1)
    }
}
