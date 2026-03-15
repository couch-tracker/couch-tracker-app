package io.github.couchtracker.error

import android.content.ContentResolver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.couchtracker.R
import io.github.couchtracker.error.ProfileDbError.FileError
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.Text
import io.github.couchtracker.utils.str
import java.io.IOException

typealias ProfileDbResult<T> = Result<T, ProfileDbError>
typealias ProfileDbLoadable<T> = Loadable<ProfileDbResult<T>>

/**
 * Represents the result of an operation on the profile database.
 */
sealed interface ProfileDbError : CouchTrackerError {

    /**
     * Indicates that the operation code encountered an exception. This could mean that:
     * 1. there is an error in the code
     * 2. the SQLite file we are trying to edit does not conform to the required specifics (e.g. missing table, column, etc.)
     */
    data class TransactionError(override val cause: Exception) : ProfileDbError {
        override val debugMessage = "Transaction error"
        override val title = Text.Resource(R.string.db_completed_error)
        override val details = null
        override val isRetriable = true
    }

    /**
     * This error indicated that there was an error retrieving the metadata of an external file, like the last modified instant.
     */
    data object MetadataError : ProfileDbError {
        override val debugMessage = "Metadata error"
        override val cause = null
        override val title = Text.Resource(R.string.db_metadata_error)
        override val details = null
        override val isRetriable = true
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
            override val debugMessage = "Content provider failure"
            override val cause = null
            override val title = Text.Lambda {
                formatOpenFileError(attemptedOperation, R.string.db_content_provider_error.str())
            }
            override val details = null
            override val isRetriable = true
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
        ) : FileError {
            override val debugMessage = "Uri cannot be opened error"
            override val title = Text.Lambda {
                formatOpenFileError(
                    attemptedOperation = attemptedOperation,
                    message = when (reason) {
                        Reason.FILE_NOT_FOUND -> R.string.db_cant_open_error_file_not_found
                        Reason.SECURITY -> R.string.db_cant_open_error_security
                    }.str(),
                )
            }
            override val details = null
            override val isRetriable = true

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
        ) : FileError {
            override val debugMessage = "Input output error"
            override val title = Text.Lambda {
                formatOpenFileError(attemptedOperation, R.string.db_io_error.str())
            }
            override val details = null
            override val isRetriable = true
        }

        /**
         * Indicates that the file selected by the user is not a valid SQLite database, and thus cannot be opened.
         *
         * This can be because the file is not a database at all (e.g. the user selected an image), or because the database is corrupted.
         */
        data object InvalidDatabase : FileError {
            override val debugMessage = "Invalid database error"
            override val cause = null
            override val title = Text.Resource(R.string.db_invalid_database_error)
            override val details = null
            override val isRetriable = true
        }
    }
}

@Composable
@ReadOnlyComposable
private fun formatOpenFileError(
    attemptedOperation: FileError.AttemptedOperation,
    message: String,
): String {
    val stringRes = when (attemptedOperation) {
        FileError.AttemptedOperation.READ -> R.string.db_file_error_read
        FileError.AttemptedOperation.WRITE -> R.string.db_file_error_write
    }
    return stringRes.str(message)
}
