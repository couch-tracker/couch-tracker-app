package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter

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
