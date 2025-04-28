package io.github.couchtracker.db.profile.model.icon

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrokenImage
import io.github.couchtracker.db.profile.CouchTrackerUri
import io.github.couchtracker.utils.Icon
import io.github.couchtracker.utils.pathSegments
import io.github.couchtracker.utils.toIcon

/**
 * Represents an icon that can be stored in the database
 */
sealed interface DbIcon {

    val icon: Icon

    fun toCouchTrackerUri(): CouchTrackerUri

    data class Default(val defaultIcon: DbDefaultIcon) : DbIcon {
        override val icon get() = defaultIcon.icon
        override fun toCouchTrackerUri() = CouchTrackerUri.Authority.ICON.uri("$DEFAULT_ICON_PATH/${defaultIcon.id}")
    }

    data class UnknownDefault(val originalUri: CouchTrackerUri) : DbIcon {
        override val icon get() = Icons.Default.BrokenImage.toIcon()
        override fun toCouchTrackerUri() = originalUri
    }

    companion object {
        private const val DEFAULT_ICON_PATH = "default"

        fun fromUri(uri: CouchTrackerUri): DbIcon {
            return fromCouchTrackerUriOrNull(uri) ?: UnknownDefault(uri)
        }

        private fun fromCouchTrackerUriOrNull(ctUri: CouchTrackerUri): DbIcon? {
            require(ctUri.authority == CouchTrackerUri.Authority.ICON) {
                "Invalid couch-tracker authority for icon: ${ctUri.authority}"
            }
            val segments = ctUri.uri.pathSegments()
            if (segments.firstOrNull() == DEFAULT_ICON_PATH) {
                require(segments.size == 2) { "Default icon must have exactly two path segments" }
                val defaultIconName = segments[1]
                require(defaultIconName.isNotBlank()) { "Default icon name cannot be blank" }
                return DbDefaultIcon.entries
                    .find { it.id == defaultIconName }
                    ?.let { Default(it) }
            }
            return null
        }
    }
}
