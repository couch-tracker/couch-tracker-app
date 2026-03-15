package io.github.couchtracker.utils.error

import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text

typealias CouchTrackerResult<T> = Result<T, CouchTrackerError>
typealias CouchTrackerLoadable<T> = Loadable<CouchTrackerResult<T>>

/**
 * Errors that can are raised commonly across the app, and should be gracefully handled and displayed to users
 */
interface CouchTrackerError {
    val debugMessage: String
    val cause: Exception?
    val title: Text
    val details: Text?
    val isRetriable: Boolean
}

/**
 * An exception that wraps [CouchTrackerError]
 */
class CouchTrackerException(val error: CouchTrackerError) : Exception(error.debugMessage, error.cause)
