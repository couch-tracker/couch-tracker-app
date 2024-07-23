package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant

object InstantColumnAdapter : ColumnAdapter<Instant, String> {
    override fun encode(value: Instant) = value.toString()
    override fun decode(databaseValue: String) = Instant.parse(databaseValue)
}
