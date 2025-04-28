package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.WatchableExternalId

/**
 * Implementation of a [ColumnAdapter] that allows to save and extract [WatchableExternalId]s in the database.
 */
object WatchableExternalIdColumnAdapter : ColumnAdapter<WatchableExternalId, String> {

    override fun decode(databaseValue: String): WatchableExternalId {
        return WatchableExternalId.parse(databaseValue)
    }

    override fun encode(value: WatchableExternalId): String {
        return value.serialize()
    }
}
