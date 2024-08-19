package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.utils.Uri
import io.github.couchtracker.utils.parseUri

object UriColumnAdapter : ColumnAdapter<Uri, String> {
    override fun encode(value: Uri) = value.toString()
    override fun decode(databaseValue: String): Uri = parseUri(databaseValue)
}
