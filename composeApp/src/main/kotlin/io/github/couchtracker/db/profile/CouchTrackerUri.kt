package io.github.couchtracker.db.profile

import io.github.couchtracker.db.profile.CouchTrackerUri.Authority
import io.github.couchtracker.db.profile.CouchTrackerUri.Companion.SCHEME
import java.net.URI

/**
 * Represents an [URI] with the `couch-tracker` scheme.
 *
 * A valid couch-tracker URI must have the correct [SCHEME] and one of the supported [Authority].
 */
@JvmInline
value class CouchTrackerUri(val uri: URI) {

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
        TEXT,
        ;

        val id = name.lowercase()

        fun uri(postfix: String) = CouchTrackerUri(URI("$SCHEME://$id/$postfix"))
    }

    companion object {
        const val SCHEME = "couch-tracker"
    }
}

private fun URI.authorityOrNull() = Authority.entries.find { it.id == authority }
