package io.github.couchtracker.utils

import android.util.Log
import io.github.couchtracker.R
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

typealias ApiResult<T> = Result<T, ApiException>
typealias ApiLoadable<T> = Loadable<ApiResult<T>>
typealias DeferredApiResult<T> = Deferred<ApiResult<T>>
typealias CompletableApiResult<T> = CompletableDeferred<ApiResult<T>>

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

    class DeserializationError(message: String?, override val cause: SerializationException?) : ApiException(message, cause) {
        override val title = Text.Resource(R.string.api_exception_deserialization_error)
    }

    class SimulatedError : ApiException("Simulated error", cause = null) {
        override val title = Text.Resource(R.string.api_exception_client_error)
    }
}

fun <T> CompletableApiResult(): CompletableApiResult<T> = CompletableDeferred()

inline fun <T> runApiCatching(logTag: String?, f: () -> T): ApiResult<T> {
    return try {
        Result.Value(f())
    } catch (e: ApiException) {
        Log.e(logTag, null, e)
        Result.Error(e)
    }
}
