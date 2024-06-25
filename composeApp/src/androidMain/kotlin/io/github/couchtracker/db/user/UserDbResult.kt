package io.github.couchtracker.db.user

import android.content.ContentResolver
import java.io.IOException

/**
 * Represents the result of an operation on the user database.
 */
sealed interface UserDbResult<out T> {

    /**
     * Indicates that the operation completed without any unexpected error from the DB perspective.
     *
     * This could still mean that there were some problems within the operation itself, but there were no problems reading/writing to the
     * (possibly external) DB. This should always be the case when the DB is managed internally in the app.
     */
    sealed interface Completed<out T> : UserDbResult<T> {

        /**
         * The operation was successful.
         * @property result the result of the operation
         */
        data class Success<out T>(val result: T) : Completed<T>

        /**
         * Indicates that the operation code encountered an exception. This could mean that:
         * 1. there is an error in the code
         * 2. the SQLite file we are trying to edit does not conform to the required specifics (e.g. missing table, column, etc.)
         */
        data class Error(val exception: Exception) : Completed<Nothing>
    }

    /**
     * Represents some kind of error reading/writing the database file.
     *
     * This can mean either a failure in the [ContentResolver] or because the file is invalid in some way (e.g. not an SQLite file).
     */
    sealed interface FileError : UserDbResult<Nothing> {

        enum class AttemptedOperation { READ, WRITE }

        /**
         * Indicates that the content provider for the URI crashed.
         *
         * @see ContentResolver.openInputStream
         * @see ContentResolver.openOutputStream
         */
        data class ContentProviderFailure(val attemptedOperation: AttemptedOperation) : FileError

        /**
         * Indicates that the given URI could not be opened, either for read or for write.
         *
         * @property exception the exception that was thrown while trying to open the URI
         * @property attemptedOperation if the problem occurred trying to read or write the URI
         * @see ContentResolver.openInputStream
         * @see ContentResolver.openOutputStream
         */
        data class UriCannotBeOpened(
            val exception: Exception,
            val attemptedOperation: AttemptedOperation,
        ) : FileError

        /**
         * Indicates that there was a problem while reading/writing to the database.
         *
         * @property exception the exception that was thrown while performing IO on the database file
         * @property attemptedOperation if the problem occurred trying to read or write
         */
        data class InputOutputError(
            val exception: IOException,
            val attemptedOperation: AttemptedOperation,
        ) : FileError

        /**
         * Indicates that the file selected by the user is not a valid SQLite database, and thus cannot be opened.
         *
         * This can be because the file is not a database at all (e.g. the user selected an image), or because the database is corrupted.
         */
        data object InvalidDatabase : FileError
    }
}

/**
 * TODO: remove, for debug only
 */
fun <T> UserDbResult<T>.debugSuccessOr(fallback: () -> T): T {
    return when (this) {
        is UserDbResult.Completed.Success -> result
        else -> fallback()
    }
}

/**
 * Maps the value of a [UserDbResult.Completed.Success] from type [T] to type [R].
 *
 * It [this] is of any other type, returns [this]
 */
fun <T, R> UserDbResult<T>.map(block: (T) -> R): UserDbResult<R> {
    return when (this) {
        is UserDbResult.Completed.Success -> UserDbResult.Completed.Success(block(result))
        is UserDbResult.Completed.Error -> this
        is UserDbResult.FileError -> this
    }
}

/**
 * Calls [block] whenever [this] is not a [UserDbResult.Completed.Success].
 *
 * Being inline, allows idiomatic code such as:
 * ```kotlin
 * value.onError { return it }
 * ```
 */
inline fun <T> UserDbResult<T>.onError(block: (UserDbResult<Nothing>) -> Unit) {
    when (this) {
        is UserDbResult.Completed.Success -> {}
        is UserDbResult.Completed.Error -> block(this)
        is UserDbResult.FileError -> block(this)
    }
}
