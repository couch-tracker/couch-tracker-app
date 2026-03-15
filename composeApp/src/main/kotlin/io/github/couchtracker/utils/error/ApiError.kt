package io.github.couchtracker.utils.error

import io.github.couchtracker.R
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import io.ktor.client.plugins.ClientRequestException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

typealias ApiResult<T> = Result<T, CouchTrackerError>
typealias ApiLoadable<T> = Loadable<ApiResult<T>>

sealed interface ApiError : CouchTrackerError {

    data class ClientError(override val cause: ClientRequestException) : ApiError {
        override val debugMessage = cause.message
        override val title = Text.Resource(R.string.api_exception_client_error)
        override val details = Text.Literal(cause.message)
        override val isRetriable = true
    }

    data class ServerError(override val cause: Exception) : ApiError {
        override val debugMessage = cause.message ?: "Server error"
        override val title = Text.Resource(R.string.api_exception_server_error)
        override val details = cause.message?.let { Text.Literal(it) }
        override val isRetriable = true
    }

    data class IOError(override val cause: IOException) : ApiError {
        override val debugMessage = cause.message ?: "IO error"
        override val title = Text.Resource(R.string.api_exception_io_error)
        override val details = cause.message?.let { Text.Literal(it) }
        override val isRetriable = true
    }

    data class DeserializationError(override val cause: SerializationException) : ApiError {
        override val debugMessage = cause.message ?: "Deserialization error"
        override val title = Text.Resource(R.string.api_exception_deserialization_error)
        override val details = cause.message?.let { Text.Literal(it) }
        override val isRetriable = true
    }

    data class ItemNotFound(override val cause: Exception, val itemIdentifier: String?) : ApiError {
        override val debugMessage = "Item not found (item = $itemIdentifier)"
        override val title = Text.Resource(R.string.api_exception_item_not_found)
        override val details = if (itemIdentifier != null) {
            Text.Lambda { R.string.api_exception_item_not_found_details.str(itemIdentifier) }
        } else {
            null
        }
        override val isRetriable = false
    }

    data class Simulated(override val cause: SimulatedException? = null) : ApiError {
        override val debugMessage = "Simulated error"
        override val title = Text.Literal("Simulated error")
        override val details = Text.Literal("This is an artificial error to test the app's behavior")
        override val isRetriable = true
    }
}

class SimulatedException : Exception()
