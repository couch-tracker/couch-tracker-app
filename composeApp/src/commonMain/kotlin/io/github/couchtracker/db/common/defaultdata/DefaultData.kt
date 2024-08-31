package io.github.couchtracker.db.common.defaultdata

import app.cash.sqldelight.Transacter

/**
 * Handles inserting default data and upgrading from one version of source data to another.
 */
interface DefaultData<DB : Transacter> {

    /**
     * Inserts the default data in the DB. This must always insert default data with the latest version available.
     */
    fun insert(db: DB)

    /**
     * Upgrades the default data in the DB to [version] from the previous version.
     *
     * For upgrades where multiple version bumps are necessary, this function is invoked for each version.
     *
     * For example, if the default data version of the DB is 2, and the latest version is 5, this function will be called 3 times with
     * [version] set to `3`, `4` and `5`, in this exact order.
     */
    fun upgradeTo(db: DB, version: Int)
}
