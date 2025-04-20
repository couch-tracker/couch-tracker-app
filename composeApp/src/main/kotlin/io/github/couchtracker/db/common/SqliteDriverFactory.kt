package io.github.couchtracker.db.common

import app.cash.sqldelight.db.SqlDriver

/**
 * Factory of [SqlDriver] for the given [DbPath].
 *
 * Implementors must make sure that returned drivers throw [DBCorruptedException] if the DB file is invalid when the driver is used.
 */
interface SqliteDriverFactory {

    fun getDriver(dbPath: DbPath): SqlDriver
}
