package io.github.couchtracker.db.common.defaultdata

import app.cash.sqldelight.Transacter

/**
 * Handles the insertion and upgrade of default data for a specific database.
 */
abstract class DefaultDataHandler<DB : Transacter>(
    private val defaultData: DefaultData<DB>,
) {

    /**
     * The latest version of default data that is handled.
     */
    protected abstract val latestVersion: Int

    /**
     * Handles creation on upgrade of the default data in the given [db].
     *
     * Every DB operation is inside a transaction, so that any failure will leave the DB untouched.
     */
    fun handle(db: DB) {
        val latestVersion = latestVersion
        db.transaction {
            val currentVersion = db.getVersion()
            if (currentVersion == null) {
                defaultData.insert(db)
                db.setVersion(latestVersion)
            } else {
                if (currentVersion < latestVersion) {
                    for (version in (currentVersion + 1)..latestVersion) {
                        defaultData.upgradeTo(db, version)
                    }
                    db.setVersion(latestVersion)
                }
            }
        }
    }

    /**
     * Gets the current default data version from the database.
     *
     * Must return `null` if the default data has never been initialized for the given database.
     */
    protected abstract fun DB.getVersion(): Int?

    /**
     * Sets the default data version in this db to the given [version].
     */
    protected abstract fun DB.setVersion(version: Int)
}
