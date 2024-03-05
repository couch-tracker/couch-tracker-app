package io.github.couchtracker.db.user

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.User
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.user.UserDbResult.FileError.AttemptedOperation
import io.github.couchtracker.db.user.show.ExternalShowId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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
sealed class UserDb {

    protected abstract suspend fun <T> doTransaction(context: Context, block: DatabaseTransaction<TransactionResult<T>>): UserDbResult<T>

    /**
     * Function that must be called when the associated user is removed from the app.
     *
     * Implementors can choose what to do with the database file in this case.
     */
    abstract suspend fun unlink(context: Context)

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
        context: Context,
        coroutineContext: CoroutineContext = Dispatchers.IO,
        block: DatabaseTransaction<TransactionResult<T>>,
    ): UserDbResult<T> {
        return withContext(coroutineContext) {
            doTransaction(context, block)
        }
    }

    /**
     * Same as [transaction], but for a transaction which only reads from the DB.
     */
    suspend fun <T> read(context: Context, block: DatabaseTransaction<T>) = transaction(context) { db ->
        TransactionResult(
            edited = false,
            result = block(db),
        )
    }

    /**
     * Same as [transaction], but for a transaction which always writes to the DB.
     */
    suspend fun <T> write(context: Context, block: DatabaseTransaction<T>) = transaction(context) { db ->
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
     * Internally thrown when the DB is corrupted. Should never be exposed outside of this class.
     */
    private class DBCorruptedException : Exception()

    companion object {
        const val LOG_TAG = "UserDb"

        /**
         * Opens the local user database given by [dbPath] and executes the transaction [block].
         *
         * Depending on the outcome of [block], it can return [UserDbResult.Completed.Success] or [UserDbResult.Completed.Error].
         *
         * If the database file is invalid (e.g. corrupted, file is not a database), [UserDbResult.FileError.InvalidDatabase] is returned.
         */
        @JvmStatic
        protected suspend fun <T> openAndUseDatabase(
            context: Context,
            dbPath: DbPath,
            onCorrupted: () -> Unit,
            block: DatabaseTransaction<T>,
        ): UserDbResult<T> {
            val driver = AndroidSqliteDriver(
                schema = UserData.Schema,
                context = context,
                name = dbPath.name,
                callback = object : AndroidSqliteDriver.Callback(UserData.Schema) {
                    override fun onCorruption(db: SupportSQLiteDatabase) {
                        Log.e(LOG_TAG, "Database $dbPath is corrupted!")
                        // Do not call super.onCorruption() here!
                        // It will delete the file and later in the process a new empty DB will be created, which is not what we want
                        throw DBCorruptedException()
                    }
                },
            )
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
        @JvmStatic
        protected fun Uri.copyToInternal(appContext: Context, file: File): UserDbResult<Unit> {
            Log.d(LOG_TAG, "Copying external file $this to internal $file")
            return openResource(uri = this, AttemptedOperation.READ, appContext.contentResolver::openInputStream) { input ->
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
        @JvmStatic
        protected fun File.copyToExternal(appContext: Context, uri: Uri): UserDbResult<Unit> {
            Log.d(LOG_TAG, "Copying internal file $this to external $uri")
            return openResource(uri, AttemptedOperation.WRITE, appContext.contentResolver::openOutputStream) { output ->
                this.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }

        /**
         * Helper function to open an input or output stream of an [uri] and handling error cases.
         */
        private fun <T : Closeable> openResource(
            uri: Uri,
            operation: AttemptedOperation,
            get: (Uri) -> T?,
            block: (T) -> Unit,
        ): UserDbResult<Unit> {
            return try {
                val resource = get(uri) ?: return UserDbResult.FileError.ContentProviderFailure(operation).also {
                    Log.w(LOG_TAG, "Unable to open $uri for $operation. Content provider returned null when opening stream")
                }
                resource.use(block)
                UserDbResult.Completed.Success(Unit)
            } catch (e: FileNotFoundException) {
                Log.w(LOG_TAG, "Unable to open $uri for $operation")
                Log.w(LOG_TAG, e)
                UserDbResult.FileError.UriCannotBeOpened(e, operation)
            }
        }
    }
}

/**
 * Returns the appropriate instance of [UserDb], depending on whether the DB is managed or external.
 *
 * @see ManagedUserDb
 * @see ExternalUserDb
 */
fun User.db(context: Context, appData: AppData): UserDb {
    return when (externalFileUri) {
        null -> ManagedUserDb(user = this, appData = appData)
        else -> ExternalUserDb.of(context = context, user = this, appData = appData)
    }
}
