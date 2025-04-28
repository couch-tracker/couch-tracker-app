package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.model.partialtime.PartialDateTime

object PartialDateTimeColumnAdapter : ColumnAdapter<PartialDateTime, String> {
    override fun decode(databaseValue: String) = PartialDateTime.parse(databaseValue)
    override fun encode(value: PartialDateTime) = value.serialize()
}
