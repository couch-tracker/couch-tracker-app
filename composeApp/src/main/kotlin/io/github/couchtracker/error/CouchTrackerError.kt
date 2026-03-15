package io.github.couchtracker.error

import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalId
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str

typealias CouchTrackerResult<T> = Result<T, CouchTrackerError>
typealias CouchTrackerLoadable<T> = Loadable<CouchTrackerResult<T>>

/**
 * Errors that can are raised commonly across the app, and should be gracefully handled and displayed to users
 */
sealed interface CouchTrackerError {
    val debugMessage: String
    val cause: Exception?
    val title: Text
    val details: Text?
    val isRetriable: Boolean

    data class UnsupportedItem(val unsupportedItem: ExternalId) : CouchTrackerError {
        override val debugMessage = "Unsupported item"
        override val cause = null
        override val title = Text.Resource(R.string.unsupported_item_id)
        override val details = Text.Lambda { R.string.unsupported_item_id_description.str(unsupportedItem.serialize()) }
        override val isRetriable = false
    }
}

/**
 * An exception that wraps [CouchTrackerError]
 */
class CouchTrackerException(val error: CouchTrackerError) : Exception(error.debugMessage, error.cause)
