package io.github.couchtracker.utils

import io.github.couchtracker.R
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

sealed class ApiException(message: String?, cause: Throwable?) : Exception(message, cause) {
    @Deprecated("Use title instead", ReplaceWith("title.string()"))
    override val message: String?
        get() = super.message

    abstract val title: Text
    open val details: Text? = message?.let { Text.Literal(it) }

    class ClientError(message: String, override val cause: ClientRequestException) : ApiException(message, cause) {
        override val title = Text.Resource(R.string.api_exception_client_error)
    }

    class ServerError(message: String?, override val cause: ResponseException) : ApiException(message, cause) {
        override val title = Text.Resource(R.string.api_exception_server_error)
    }

    class IOError(message: String?, override val cause: IOException) : ApiException(message, cause) {
        override val title = Text.Resource(R.string.api_exception_io_error)
    }

    class DeserializationError(message: String?, override val cause: SerializationException) : ApiException(message, cause) {
        override val title = Text.Resource(R.string.api_exception_deserialization_error)
    }
}
