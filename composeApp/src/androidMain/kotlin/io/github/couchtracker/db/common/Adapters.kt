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

/**
 * Transforms a `ColumnAdapter<I, T>` to a `ColumnAdapter<O, T>`.
 * Transformations are performed using the given [encoder] and [decoder].
 */
fun <T, I : Any, O : Any> ColumnAdapter<I, T>.map(
    encoder: (O) -> I,
    decoder: (I) -> O,
): ColumnAdapter<O, T> {
    val base = this
    return object : ColumnAdapter<O, T> {
        override fun decode(databaseValue: T): O = decoder(base.decode(databaseValue))
        override fun encode(value: O): T = base.encode(encoder(value))
    }
}
