package io.github.couchtracker.db.app

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.common.adapters.InstantColumnAdapter
import io.github.couchtracker.db.common.adapters.URIColumnAdapter
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val AppDataModule = module {
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
}
