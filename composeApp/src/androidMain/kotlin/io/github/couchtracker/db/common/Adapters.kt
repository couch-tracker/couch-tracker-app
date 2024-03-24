package io.github.couchtracker.db.common

import android.net.Uri
import app.cash.sqldelight.ColumnAdapter
import kotlinx.datetime.Instant
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object InstantColumnAdapter : ColumnAdapter<Instant, String> {
    override fun encode(value: Instant) = value.toString()
    override fun decode(databaseValue: String) = Instant.parse(databaseValue)
}

object UriColumnAdapter : ColumnAdapter<Uri, String> {
    override fun encode(value: Uri) = value.toString()
    override fun decode(databaseValue: String): Uri = Uri.parse(databaseValue)
}

inline fun <reified T : Any> jsonAdapter() = object : ColumnAdapter<T, String> {
    override fun decode(databaseValue: String) = Json.decodeFromString<T>(databaseValue)
    override fun encode(value: T) = Json.encodeToString(value)
}
