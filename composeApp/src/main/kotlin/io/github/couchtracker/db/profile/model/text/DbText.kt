package io.github.couchtracker.db.profile.model.text

import io.github.couchtracker.db.profile.CouchTrackerUri
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.encodeUriQuery
import io.github.couchtracker.utils.pathSegments
import io.github.couchtracker.utils.toText

/**
 * Represents a string that can be stored in the database
 */
sealed interface DbText {

    val text: Text

    fun toCouchTrackerUri(): CouchTrackerUri

    data class Default(val defaultText: DbDefaultText) : DbText {
        override val text get() = defaultText.text
        override fun toCouchTrackerUri() = CouchTrackerUri.Authority.TEXT.uri("$DEFAULT_TEXT_PATH/${defaultText.id}")
    }

    data class UnknownDefault(val id: String, val originalUri: CouchTrackerUri) : DbText {
        override val text get() = id.toText()
        override fun toCouchTrackerUri() = originalUri
    }

    data class Custom(val value: String) : DbText {
        override val text get() = value.toText()
        override fun toCouchTrackerUri() = CouchTrackerUri.Authority.TEXT.uri("$CUSTOM_TEXT_PATH/${encodeUriQuery(value)}")
    }

    companion object {
        private const val DEFAULT_TEXT_PATH = "default"
        private const val CUSTOM_TEXT_PATH = "custom"

        fun fromUri(ctUri: CouchTrackerUri): DbText {
            require(ctUri.authority == CouchTrackerUri.Authority.TEXT) {
                "Invalid couch-tracker authority for text: ${ctUri.authority}"
            }
            val segments = ctUri.uri.pathSegments()
            when (segments.firstOrNull()) {
                DEFAULT_TEXT_PATH -> {
                    require(segments.size == 2) { "Default text must have exactly two path segments" }
                    val defaultTextName = segments[1]
                    require(defaultTextName.isNotBlank()) { "Default text name cannot be blank" }
                    return DbDefaultText.entries
                        .find { it.id == defaultTextName }
                        ?.let { Default(it) }
                        ?: UnknownDefault(defaultTextName, ctUri)
                }

                CUSTOM_TEXT_PATH -> {
                    require(segments.size == 1) { "Custom text must have exactly one path segment" }
                    val query = requireNotNull(ctUri.uri.query) { "Custom text must have query component" }
                    return Custom(query)
                }
            }
            throw IllegalArgumentException("Unparsable text URI: $ctUri")
        }
    }
}
