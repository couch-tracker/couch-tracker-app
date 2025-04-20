package io.github.couchtracker.db.common.defaultdata

import app.cash.sqldelight.Transacter

/**
 * [DefaultData] implementation that simply "merges" multiple [DefaultData] into a single one by simply iterating through them on [insert]
 * and [upgradeTo] calls.
 */
open class MultipleDefaultData<DB : Transacter>(
    private val datas: List<DefaultData<DB>>,
) : DefaultData<DB> {

    constructor(vararg providers: DefaultData<DB>) : this(providers.toList())

    override fun insert(db: DB) {
        for (provider in datas) {
            provider.insert(db)
        }
    }

    override fun upgradeTo(db: DB, version: Int) {
        for (provider in datas) {
            provider.upgradeTo(db, version)
        }
    }
}
