package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private class JsonListColumnAdapter<T : Any>(
    private val itemColumnAdapter: ColumnAdapter<T, String>,
) : ColumnAdapter<List<T>, String> {

    override fun decode(databaseValue: String): List<T> {
        return Json.decodeFromString<List<String>>(databaseValue).map { itemColumnAdapter.decode(it) }
    }

    override fun encode(value: List<T>): String {
        return Json.encodeToString(value.map { itemColumnAdapter.encode(it) })
    }
}

fun <T : Any> ColumnAdapter<T, String>.jsonList(): ColumnAdapter<List<T>, String> = JsonListColumnAdapter(this)

fun <T : Any> ColumnAdapter<T, String>.jsonSet() = jsonList().map(
    encoder = { it.toList() },
    decoder = { it.toSet() },
)
