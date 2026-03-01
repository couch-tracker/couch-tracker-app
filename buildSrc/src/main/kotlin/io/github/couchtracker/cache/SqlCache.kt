package io.github.couchtracker.cache

import io.github.couchtracker.SqlColumn
import io.github.couchtracker.SqlFunction
import io.github.couchtracker.SqlItem
import io.github.couchtracker.SqlTable

private val LAST_UPDATE_COLUMN = SqlColumn.text("lastUpdate", "kotlin.time.Instant")

internal data class SqlCache(
    val name: String,
    val keys: List<CacheKey>,
    val value: SqlColumn,
) : SqlItem {

    init {
        require(keys.isNotEmpty())
    }

    private val table = SqlTable(
        name = name,
        keys = keys.map { it.column },
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

    companion object {
        fun forTmdbItem(name: String, key: TmdbModelKey, value: SqlColumn): SqlCache {
            return SqlCache(name, listOf(key), value)
        }

        fun forLocalizedTmdbItem(name: String, key: TmdbModelKey, languageKey: LanguageKey, value: SqlColumn): SqlCache {
            return SqlCache(name, listOf(key, languageKey), value)
        }

        fun forLocalizedInformation(name: String, languageKey: LanguageKey, value: SqlColumn): SqlCache {
            return SqlCache(name, listOf(languageKey), value)
        }
    }
}
