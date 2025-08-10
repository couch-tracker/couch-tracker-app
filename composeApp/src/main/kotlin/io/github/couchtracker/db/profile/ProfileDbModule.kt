package io.github.couchtracker.db.profile

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.utils.lazyEagerModule
import org.koin.core.qualifier.named
import org.koin.dsl.bind

val ProfileDbModule = lazyEagerModule {
    includes(ProfileDbCommonModule)

    factory(named("ProfileDb")) {
        AndroidSqliteDriverFactory(schema = ProfileData.Schema)
    }.bind<SqliteDriverFactory>()
}
