package io.github.couchtracker.db.user

import io.github.couchtracker.db.user.CouchTrackerUri.Authority
import io.github.couchtracker.db.user.CouchTrackerUri.Companion.SCHEME
import io.github.couchtracker.utils.Uri
import io.github.couchtracker.utils.parseUri

/**
 * Represents an [Uri] with the `couch-tracker` scheme.
 *
 * A valid couch-tracker URI must have the correct [SCHEME] and one of the supported [Authority].
 */
@JvmInline
value class CouchTrackerUri(val uri: Uri) {

    val authority get() = requireNotNull(uri.authorityOrNull())

    init {
        require(uri.scheme == SCHEME) { "Invalid schema for couch-tracker URI: ${uri.scheme}" }
        requireNotNull(uri.authorityOrNull()) { "Invalid authority for couch-tracker URI: ${uri.authority}" }
    }

    /**
     * Represents one of the valid "types" of couch-tracker URIs.
     * @property id the authority string which must be used in the URI for this authority.
     */
    enum class Authority {
        ICON,
        ;

        val id = name.lowercase()

        fun uri(postfix: String) = CouchTrackerUri(parseUri("$SCHEME://$id/$postfix"))
    }

    companion object {
        const val SCHEME = "couch-tracker"
    }
}

private fun Uri.authorityOrNull() = Authority.entries.find { it.id == authority }
