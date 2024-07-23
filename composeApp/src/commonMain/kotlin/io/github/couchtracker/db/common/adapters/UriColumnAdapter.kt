package io.github.couchtracker.db.common.adapters

import android.net.Uri
import app.cash.sqldelight.ColumnAdapter

object UriColumnAdapter : ColumnAdapter<Uri, String> {
    override fun encode(value: Uri) = value.toString()
    override fun decode(databaseValue: String): Uri = Uri.parse(databaseValue)
}
