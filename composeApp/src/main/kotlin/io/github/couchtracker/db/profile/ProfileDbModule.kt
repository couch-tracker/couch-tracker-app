package io.github.couchtracker.db.profile

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.SqliteDriverFactory
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val ProfileDbModule = module {
    includes(ProfileDbCommonModule)

    factory(named("ProfileDb")) {
        AndroidSqliteDriverFactory(schema = ProfileData.Schema)
    }.bind<SqliteDriverFactory>()
}
