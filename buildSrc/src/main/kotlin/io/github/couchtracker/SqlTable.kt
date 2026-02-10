package io.github.couchtracker

import kotlin.collections.forEach

internal data class SqlTable(
    val name: String,
    val keys: List<SqlColumn>,
    val columns: List<SqlColumn>,
) : SqlItem {

    init {
        require(keys.isNotEmpty())
        require(keys.all { !it.nullable })
    }

    override fun toSql() = buildString {
        appendLine("CREATE TABLE $name (")
        keys.forEach { column ->
            appendLine("    ${column.columnDefinition()},")
        }
        columns.forEach { column ->
            appendLine("    ${column.columnDefinition()},")
        }
        appendLine("    PRIMARY KEY(${keys.joinToString { it.name }})")
        appendLine(");")
    }
}
