package io.github.couchtracker.intl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.db.profile.ProfileDbError.FileError
import io.github.couchtracker.db.profile.ProfileDbError.FileError.ContentProviderFailure
import io.github.couchtracker.db.profile.ProfileDbError.FileError.InvalidDatabase
import io.github.couchtracker.db.profile.ProfileDbError.FileError.UriCannotBeOpened
import io.github.couchtracker.db.profile.ProfileDbError.MetadataError
import io.github.couchtracker.utils.str

@Composable
@ReadOnlyComposable
fun ProfileDbError.errorMessage(): String {
    return when (this) {
        is ProfileDbError.TransactionError -> R.string.db_completed_error.str()
        is MetadataError -> R.string.db_metadata_error.str()
        is InvalidDatabase -> R.string.db_invalid_database_error.str()
        is ContentProviderFailure -> formatOpenFileError(attemptedOperation, R.string.db_content_provider_error.str())
        is UriCannotBeOpened -> formatOpenFileError(
            attemptedOperation = attemptedOperation,
            message = when (reason) {
                UriCannotBeOpened.Reason.FILE_NOT_FOUND -> R.string.db_cant_open_error_file_not_found
                UriCannotBeOpened.Reason.SECURITY -> R.string.db_cant_open_error_security
            }.str(),
        )

        is FileError.InputOutputError -> formatOpenFileError(attemptedOperation, R.string.db_io_error.str())
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
