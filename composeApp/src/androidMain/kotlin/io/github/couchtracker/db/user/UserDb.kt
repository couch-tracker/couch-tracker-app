package io.github.couchtracker.db.user

import android.content.Context
import android.net.Uri
import android.util.Log
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.common.DBCorruptedException
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.common.SqliteDriverFactory
import io.github.couchtracker.db.user.UserDbResult.FileError.AttemptedOperation
import io.github.couchtracker.db.user.show.ExternalShowId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import org.koin.core.qualifier.named
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import kotlin.coroutines.CoroutineContext

typealias DatabaseTransaction<T> = suspend (db: UserData) -> T

/**
 * Base class for a database containing user data.
 *
 * @see ManagedUserDb
 * @see ExternalUserDb
 */
sealed class UserDb : KoinComponent {

    protected val context = get<Context>()
    protected val appData = get<AppData>()

    protected abstract suspend fun <T> doTransaction(block: DatabaseTransaction<TransactionResult<T>>): UserDbResult<T>

    /**
     * Function that must be called when the associated user is removed from the app.
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
    ): UserDbResult<T> {
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
     * Opens the local user database given by [dbPath] and executes the transaction [block].
     *
     * Depending on the outcome of [block], it can return [UserDbResult.Completed.Success] or [UserDbResult.Completed.Error].
     *
     * If the database file is invalid (e.g. corrupted, file is not a database), [UserDbResult.FileError.InvalidDatabase] is returned.
     */
    protected suspend fun <T> openAndUseDatabase(
        dbPath: DbPath,
        onCorrupted: () -> Unit,
        block: DatabaseTransaction<T>,
    ): UserDbResult<T> {
        val driver = get<SqliteDriverFactory>(named("UserDb")).getDriver(dbPath)
        return driver.use {
            val db = UserData(
                driver = driver,
                ShowCollectionAdapter = ShowCollection.Adapter(
                    showIdAdapter = ExternalShowId.columnAdapter(),
                ),
            )
            try {
                UserDbResult.Completed.Success(result = block(db))
            } catch (ignored: DBCorruptedException) {
                onCorrupted()
                UserDbResult.FileError.InvalidDatabase
            } catch (expected: Exception) {
                Log.e(LOG_TAG, "Error in user database transaction", expected)
                UserDbResult.Completed.Error(exception = expected)
            }
        }
    }

    /**
     * Copies [this] external [Uri] document to the internal [file].
     *
     * @return `null` on success, non-`null` [UserDbResult] if there was an error
     */
    protected fun Uri.copyToInternal(file: File): UserDbResult<Unit> {
        Log.d(LOG_TAG, "Copying external file $this to internal $file")
        return openResource(uri = this, AttemptedOperation.READ, context.contentResolver::openInputStream) { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }
    }

    /**
     * Copies [this] internal [File] to the external [uri] document.
     *
     * @return `null` on success, non-`null` [UserDbResult] if there was an error
     */
    protected fun File.copyToExternal(uri: Uri): UserDbResult<Unit> {
        Log.d(LOG_TAG, "Copying internal file $this to external $uri")
        return openResource(uri, AttemptedOperation.WRITE, context.contentResolver::openOutputStream) { output ->
            this.inputStream().use { input ->
                input.copyTo(output)
            }
        }
    }

    companion object {
        const val LOG_TAG = "UserDb"

        /**
         * Helper function to open an input or output stream of an [uri] and handling error cases.
         */
        private fun <T : Closeable> openResource(
            uri: Uri,
            operation: AttemptedOperation,
            get: (Uri) -> T?,
            block: (T) -> Unit,
        ): UserDbResult<Unit> {
            fun onOpenError(e: Exception): UserDbResult.FileError.UriCannotBeOpened {
                Log.w(LOG_TAG, "Unable to open $uri for $operation", e)
                return UserDbResult.FileError.UriCannotBeOpened(e, operation)
            }

            return try {
                val resource = get(uri) ?: return UserDbResult.FileError.ContentProviderFailure(operation).also {
                    Log.w(LOG_TAG, "Unable to open $uri for $operation. Content provider returned null when opening stream")
                }
                resource.use(block)
                UserDbResult.Completed.Success(Unit)
            } catch (e: FileNotFoundException) {
                onOpenError(e)
            } catch (e: SecurityException) {
                onOpenError(e)
            }
        }
    }
}
