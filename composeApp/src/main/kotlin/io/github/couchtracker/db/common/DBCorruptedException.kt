package io.github.couchtracker.db.common

/**
 * Thrown by a driver provided by a [SqliteDriverFactory] upon usage when the DB is corrupted.
 */
class DBCorruptedException : Exception()
