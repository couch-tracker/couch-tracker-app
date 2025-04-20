package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import java.net.URI

object URIColumnAdapter : ColumnAdapter<URI, String> {
    override fun encode(value: URI) = value.toString()
    override fun decode(databaseValue: String): URI = URI(databaseValue)
}
