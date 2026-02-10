package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn
import io.github.couchtracker.SqlFunction
import io.github.couchtracker.SqlItem
import io.github.couchtracker.SqlTable

private val LANGUAGE_COLUMN = SqlColumn.text("language", "io.github.couchtracker.tmdb.TmdbLanguage")
private val LAST_UPDATE_COLUMN = SqlColumn.text("lastUpdate", "kotlin.time.Instant")

internal data class SqlTmdbModelCache(
    val name: String,
    val key: TmdbModelKey,
    val hasLanguage: Boolean,
    val value: SqlColumn,
) : SqlItem {

    private val table = SqlTable(
        name = name,
        keys = buildList {
            add(key.column)
            if (hasLanguage) {
                add(LANGUAGE_COLUMN)
            }
        },
        columns = listOf(value, LAST_UPDATE_COLUMN),
    )

    private fun getFunction() = SqlFunction("get") {
        appendLine("SELECT ${value.name}, ${LAST_UPDATE_COLUMN.name}")
        appendLine("FROM ${table.name}")
        appendLine("WHERE " + table.keys.joinToString(" AND ") { "${it.name} = ?" } + ";")
    }

    private fun putFunction() = SqlFunction("put") {
        appendLine("INSERT INTO ${table.name}")
        appendLine("VALUES (${table.keys.joinToString { ":${it.name}" }}, :${value.name}, :${LAST_UPDATE_COLUMN.name})")
        appendLine(
            "ON CONFLICT DO UPDATE SET " +
                "${value.name} = :${value.name}, " +
                "${LAST_UPDATE_COLUMN.name} = :${LAST_UPDATE_COLUMN.name};",
        )
    }

    override fun toSql() = buildString {
        append(table.toSql())
        appendLine()
        appendLine(getFunction().toSql())
        appendLine(putFunction().toSql())
    }
}
