package io.github.couchtracker.db.common.adapters

import app.cash.sqldelight.ColumnAdapter
import io.github.couchtracker.db.profile.Bcp47Language

object Bcp47LanguageColumnAdapter : ColumnAdapter<Bcp47Language, String> {

    override fun decode(databaseValue: String): Bcp47Language {
        return Bcp47Language.of(databaseValue)
    }

    override fun encode(value: Bcp47Language): String {
        return value.toString()
    }
}
