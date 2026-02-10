package io.github.couchtracker

internal data class SqlColumn(
    val name: String,
    val sqlType: String,
    val kotlinType: String,
    val nullable: Boolean = false,
) {
    fun columnDefinition() = buildString {
        append("$name $sqlType AS $kotlinType")
        if (!nullable) {
            append(" NOT NULL")
        }
    }

    companion object {
        fun int(name: String, kotlinType: String, nullable: Boolean = false) = SqlColumn(name, "INTEGER", kotlinType, nullable)
        fun text(name: String, kotlinType: String, nullable: Boolean = false) = SqlColumn(name, "TEXT", kotlinType, nullable)
    }
}
