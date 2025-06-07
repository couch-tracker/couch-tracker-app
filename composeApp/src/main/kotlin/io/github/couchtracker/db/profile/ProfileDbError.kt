package io.github.couchtracker.db.profile

import android.content.ContentResolver
import io.github.couchtracker.utils.Result
import java.io.IOException

typealias ProfileDbResult<T> = Result<T, ProfileDbError>

/**
 * Represents the result of an operation on the profile database.
 */
sealed interface ProfileDbError {

    sealed interface WithException : ProfileDbError {
        val exception: Exception
    }

    /**
     * Indicates that the operation code encountered an exception. This could mean that:
     * 1. there is an error in the code
     * 2. the SQLite file we are trying to edit does not conform to the required specifics (e.g. missing table, column, etc.)
     */
    data class TransactionError(override val exception: Exception) : ProfileDbError, WithException

    /**
     * This error indicated that there was an error retrieving the metadata of an external file, like the last modified instant.
     */
    data object MetadataError : ProfileDbError

    /**
     * Represents some kind of error reading/writing the database file.
     *
     * This can mean either a failure in Android's `ContentResolver` or because the file is invalid in some way (e.g. not an SQLite file).
     */
    sealed interface FileError : ProfileDbError {

        enum class AttemptedOperation { READ, WRITE }

        /**
         * Indicates that the content provider for the URI crashed.
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
            override val exception: Exception,
            val reason: Reason,
            val attemptedOperation: AttemptedOperation,
        ) : FileError, WithException {

            enum class Reason {
                FILE_NOT_FOUND,
                SECURITY,
            }
        }

        /**
         * Indicates that there was a problem while reading/writing to the database.
         *
         * @property exception the exception that was thrown while performing IO on the database file
         * @property attemptedOperation if the problem occurred trying to read or write
         */
        data class InputOutputError(
            override val exception: IOException,
            val attemptedOperation: AttemptedOperation,
        ) : FileError, WithException

        /**
         * Indicates that the file selected by the user is not a valid SQLite database, and thus cannot be opened.
         *
         * This can be because the file is not a database at all (e.g. the user selected an image), or because the database is corrupted.
         */
        data object InvalidDatabase : FileError
    }
}
