package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter

/**
 * Column adapter that allows to store Int values in a TEXT SQLite field.
 *
 * This is useful for the Metadata table, where we can have values of different types in the same column.
 */
object TextIntColumnAdapter : ColumnAdapter<Int, String> {
    override fun encode(value: Int) = value.toString()
    override fun decode(databaseValue: String) = databaseValue.toInt()
}
