package io.github.couchtracker

import kotlin.text.appendLine

internal data class SqlFunction(
    val name: String,
    val content: String,
) : SqlItem {

    constructor(name: String, content: StringBuilder.() -> Unit) : this(name, buildString { content() })

    override fun toSql() = buildString {
        appendLine("${name}:")
        appendLine(content)
    }
}
