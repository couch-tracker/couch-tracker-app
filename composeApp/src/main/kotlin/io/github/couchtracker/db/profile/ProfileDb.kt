package io.github.couchtracker.db.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.common.DBCorruptedException
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.profile.ProfileDbError.FileError.AttemptedOperation
import io.github.couchtracker.utils.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.parameter.parametersOf
import org.koin.core.qualifier.named
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.net.URI
import kotlin.coroutines.CoroutineContext

typealias DatabaseTransaction<T> = suspend (db: ProfileData) -> T

/**
 * Base class for a database containing profile data.
 *
 * @see ManagedProfileDb
 * @see ExternalProfileDb
 */
sealed class ProfileDb : KoinComponent {

    protected val context = get<Context>()
    protected val appData = get<AppData>()

    protected abstract suspend fun <T> doTransaction(block: DatabaseTransaction<TransactionResult<T>>): ProfileDbResult<T>

    /**
     * Size of the file, in bytes. `null` if it's unknown
     */
    abstract fun size(): Long?

    /**
     * [Instant] of when the DB was last written to. `null` if it's unknown
     */
    abstract fun lastModified(): Instant?

    /**
     * Function that must be called when the associated profile is removed from the app.
     *
     * Implementors can choose what to do with the database file in this case.
     */
    abstract suspend fun unlink()

    /**
     * Performs a transaction on the current database.
     *
     * This function will open the database, perform [block] and close it.
     *
     * As the database is opened from scratch, [block] shouldn't rely on previously loaded data, or at least handle the case where the data
     * changed gracefully.
     *
     * [block] could be called more than once in some scenarios, so the function shouldn't have any side effects outside the database.
     * Multiple calls to [block] should perform the same action and return the same result.
     *
     * @see read
     * @see write
     */
    suspend fun <T> transaction(
        coroutineContext: CoroutineContext = Dispatchers.IO,
        block: DatabaseTransaction<TransactionResult<T>>,
    ): ProfileDbResult<T> {
        return withContext(coroutineContext) {
            doTransaction(block)
        }
    }

    /**
     * Same as [transaction], but for a transaction which only reads from the DB.
     */
    suspend fun <T> read(block: DatabaseTransaction<T>) = transaction { db ->
        TransactionResult(
            edited = false,
            result = block(db),
        )
    }

    /**
     * Same as [transaction], but for a transaction which always writes to the DB.
     */
    suspend fun <T> write(block: DatabaseTransaction<T>) = transaction { db ->
        TransactionResult(
            edited = true,
            result = block(db),
        )
    }

    suspend fun ensureDbExists() = write { db ->
        db.transaction {
            // We need to execute an empty DB transaction in order to force the creation of the DB in case it doesn't exist
        }
    }

    /**
     * The result of a [transaction].
     *
     * @property edited indicates whether the DB was modified by the transaction. MUST be true if a change happened.
     * @property result the result retrieved by the DB.
     */
    data class TransactionResult<out T>(
        val edited: Boolean,
        val result: T,
    )

    /**
     * Opens the local profile database given by [dbPath] and executes the transaction [block].
     *
     * If [block] fails, it returns a [ProfileDbError.TransactionError].
     *
     * If the database file is invalid (e.g. corrupted, file is not a database), [ProfileDbError.FileError.InvalidDatabase] is returned.
     */
    protected suspend fun <T> openAndUseDatabase(
        dbPath: DbPath,
        onCorrupted: () -> Unit,
        block: DatabaseTransaction<T>,
    ): ProfileDbResult<T> {
        val driver = get<SqliteDriverFactory>(named("ProfileDb")).getDriver(dbPath)
        return driver.use {
            try {
                val db = get<ProfileData> { parametersOf(driver) }
                Result.Value(value = block(db))
            } catch (ignored: DBCorruptedException) {
                onCorrupted()
                Result.Error(ProfileDbError.FileError.InvalidDatabase)
            } catch (expected: Exception) {
                Log.e(LOG_TAG, "Error in profile database transaction", expected)
                Result.Error(ProfileDbError.TransactionError(exception = expected))
            }
        }
    }

    /**
     * Copies [this] external [URI] document to the internal [file].
     */
    protected fun Uri.copyToInternal(file: File): ProfileDbResult<Unit> {
        Log.d(LOG_TAG, "Copying external file $this to internal $file")
        return openResource(uri = this, AttemptedOperation.READ, context.contentResolver::openInputStream) { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Copies [this] internal [File] to the external [uri] document.
     */
    protected fun File.copyToExternal(uri: Uri): ProfileDbResult<Unit> {
        Log.d(LOG_TAG, "Copying internal file $this to external $uri")
        return openResource(uri, AttemptedOperation.WRITE, context.contentResolver::openOutputStream) { output ->
            this.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        const val LOG_TAG = "ProfileDb"

        /**
         * Helper function to open an input or output stream of an [uri] and handling error cases.
         */
        private fun <T : Closeable> openResource(
            uri: Uri,
            operation: AttemptedOperation,
            get: (Uri) -> T?,
            block: (T) -> Unit,
        ): ProfileDbResult<Unit> {
            fun onOpenError(
                exception: Exception,
                reason: ProfileDbError.FileError.UriCannotBeOpened.Reason,
            ): Result.Error<ProfileDbError.FileError.UriCannotBeOpened> {
                Log.w(LOG_TAG, "Unable to open $uri for $operation", exception)
                return Result.Error(ProfileDbError.FileError.UriCannotBeOpened(exception, reason, operation))
            }

            val resource = try {
                get(uri) ?: return Result.Error(
                    ProfileDbError.FileError.ContentProviderFailure(operation).also {
                        Log.w(LOG_TAG, "Unable to open $uri for $operation. Content provider returned null when opening stream")
                    },
                )
            } catch (e: FileNotFoundException) {
                return onOpenError(e, ProfileDbError.FileError.UriCannotBeOpened.Reason.FILE_NOT_FOUND)
            } catch (e: SecurityException) {
                return onOpenError(e, ProfileDbError.FileError.UriCannotBeOpened.Reason.SECURITY)
            }
            return try {
                resource.use(block)
                Result.Value(Unit)
            } catch (e: IOException) {
                Log.w(LOG_TAG, "IO error while executing $operation on $uri", e)
                Result.Error(ProfileDbError.FileError.InputOutputError(e, operation))
            }
        }
    }
}
