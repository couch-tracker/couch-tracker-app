package io.github.couchtracker.db.user.model.text

import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.toText

/**
 * Enum class of all available texts supported by default by this app.
 *
 * @property id the ID of this text that will be used in its serialization.
 */
enum class DbDefaultText(val text: Text) {

    HOME("Home".toText()), // TODO translate
    PLANE("Plane".toText()), // TODO translate
    ;

    val id = name.lowercase().replace('_', '-')

    fun toDbText() = DbText.Default(this)
}
