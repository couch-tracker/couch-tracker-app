package io.github.couchtracker.db.user

import io.github.couchtracker.db.common.AndroidSqliteDriverFactory
import io.github.couchtracker.db.common.SqliteDriverFactory
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val UserDbModule = module {
    factory(named("UserDb")) {
        AndroidSqliteDriverFactory(schema = UserData.Schema)
    }.bind<SqliteDriverFactory>()
}
