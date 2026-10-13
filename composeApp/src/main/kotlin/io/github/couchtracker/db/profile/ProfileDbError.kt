package io.github.couchtracker.db.profile

import android.content.ContentResolver
import androidx.annotation.StringRes
import io.github.couchtracker.R
import io.github.couchtracker.db.common.DBCorruptedException
import io.github.couchtracker.db.profile.ProfileDbError.FileError
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.error.CouchTrackerError
import io.github.couchtracker.utils.str
import java.io.IOException

typealias ProfileDbResult<T> = Result<T, ProfileDbError>
typealias ProfileDbLoadable<T> = Loadable<ProfileDbResult<T>>

/**
 * Represents the result of an operation on the profile database.
 */
sealed interface ProfileDbError : CouchTrackerError {

    sealed interface WithException : ProfileDbError {
        override val cause: Exception
    }

    /**
     * Indicates that the operation code encountered an exception. This could mean that:
     * 1. there is an error in the code
     * 2. the SQLite file we are trying to edit does not conform to the required specifics (e.g. missing table, column, etc.)
     */
    data class TransactionError(override val cause: Exception) : ProfileDbError, WithException {
        override val debugMessage = cause.message ?: "TransactionError"
        override val title = Text.Resource(R.string.db_completed_error)
        override val details = cause.message?.let { Text.Literal(it) }
        override val isRetriable = false
        override val requiresUserAttention = true
    }

    /**
     * This error indicates that there was an error retrieving the metadata of an external file, like the last modified instant.
     */
    data object MetadataError : ProfileDbError {
        override val debugMessage = "MetadataError"
        override val cause = null
        override val title = Text.Resource(R.string.db_metadata_error)
        override val details = null
        override val isRetriable = false
        override val requiresUserAttention = true
    }

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
        data class ContentProviderFailure(val attemptedOperation: AttemptedOperation) : FileError {
            override val debugMessage = "ContentProviderFailure($attemptedOperation)"
            override val cause = null
            override val title = openFileError(attemptedOperation, R.string.db_content_provider_error)
            override val details = null
            override val isRetriable = true
            override val requiresUserAttention = false
        }

        /**
         * Indicates that the given URI could not be opened, either for read or for write.
         *
         * @property cause the exception that was thrown while trying to open the URI
         * @property attemptedOperation if the problem occurred trying to read or write the URI
         * @see ContentResolver.openInputStream
         * @see ContentResolver.openOutputStream
         */
        data class UriCannotBeOpened(
            override val cause: Exception,
            val reason: Reason,
            val attemptedOperation: AttemptedOperation,
        ) : FileError, WithException {
            override val debugMessage = cause.message ?: "UriCannotBeOpened"
            override val title = openFileError(
                attemptedOperation = attemptedOperation,
                messageRes = when (reason) {
                    Reason.FILE_NOT_FOUND -> R.string.db_cant_open_error_file_not_found
                    Reason.SECURITY -> R.string.db_cant_open_error_security
                },
            )
            override val details = cause.message?.let { Text.Literal(it) }
            override val isRetriable = true
            override val requiresUserAttention = true

            enum class Reason {
                FILE_NOT_FOUND,
                SECURITY,
            }
        }

        /**
         * Indicates that there was a problem while reading/writing to the database.
         *
         * @property cause the exception that was thrown while performing IO on the database file
         * @property attemptedOperation if the problem occurred trying to read or write
         */
        data class InputOutputError(
            override val cause: IOException,
            val attemptedOperation: AttemptedOperation,
        ) : FileError, WithException {
            override val debugMessage = cause.message ?: "InputOutputError"
            override val title = openFileError(attemptedOperation, R.string.db_io_error)
            override val details = cause.message?.let { Text.Literal(it) }
            override val isRetriable = true
            override val requiresUserAttention = false
        }

        /**
         * Indicates that the file selected by the user is not a valid SQLite database, and thus cannot be opened.
         *
         * This can be because the file is not a database at all (e.g. the user selected an image), or because the database is corrupted.
         */
        data class InvalidDatabase(override val cause: DBCorruptedException) : WithException, FileError {
            override val debugMessage = cause.message ?: "InvalidDatabase"
            override val details = cause.message?.let { Text.Literal(it) }
            override val title = Text.Resource(R.string.db_invalid_database_error)
            override val isRetriable = true
            override val requiresUserAttention = true
        }
    }
}

private fun openFileError(
    attemptedOperation: FileError.AttemptedOperation,
    @StringRes messageRes: Int,
) = Text.Lambda {
    val stringRes = when (attemptedOperation) {
        FileError.AttemptedOperation.READ -> R.string.db_file_error_read
        FileError.AttemptedOperation.WRITE -> R.string.db_file_error_write
    }
    stringRes.str(messageRes.str())
}
