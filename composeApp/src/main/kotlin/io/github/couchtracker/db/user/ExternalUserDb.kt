package io.github.couchtracker.db.user

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.lastModifiedInstant
import io.github.couchtracker.db.toDocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import java.net.URI
import kotlin.coroutines.CoroutineContext

/**
 * A [UserDb] that is managed in an external file.
 *
 * The [transaction] function of this class takes care of copying the external file to internal storage before opening it, and then copying
 * it back, when necessary.
 */
class ExternalUserDb private constructor(
    private val userId: Long,
    private val cachedDb: DbPath,
    private val externalDb: DocumentFile,
) : UserDb() {

    override suspend fun <T> doTransaction(block: DatabaseTransaction<TransactionResult<T>>): UserDbResult<T> {
        // Check if we have an up-to-date cached copy of the database. If not, copy it to internal storage
        val externalLastModified = externalDb.lastModifiedInstant()
        when (isCachedDatabaseUpToDate(externalLastModified)) {
            null -> {
                // there was an error in retrieving the last cached date
                return UserDbResult.InterruptedError
            }

            true -> {
                Log.i(LOG_TAG, "Cached database is up to date, no copy needed")
            }

            false -> {
                Log.i(LOG_TAG, "Cached database is not up to date or doesn't exist, copying to internal storage")
                externalDb.uri.copyToInternal(cachedDb.file).onError { return it }
                updateCachedLastModified(externalLastModified)
            }
        }

        // Execute transaction on cached file
        val dbResult = openAndUseDatabase(
            dbPath = cachedDb,
            onCorrupted = {
                // this probably means that the external file is not a valid SQLite file
                // we'll just delete the cached file
                cleanCached()
            },
            block = block,
        )

        if (dbResult is UserDbResult.Completed.Success) {
            // The transaction completed successfully on the local file

            if (dbResult.result.edited) {
                // Check the last modified date of the external file again, as it might have changed while we were running the transaction
                val currentExternalLastModified = externalDb.lastModifiedInstant()
                Log.i(LOG_TAG, "Old last modified: $externalLastModified, external last modified: $currentExternalLastModified")
                if (currentExternalLastModified != externalLastModified) {
                    Log.i(LOG_TAG, "External file changed while transaction was running!")
                    // We cannot copy the newly modified file over, or we'll end overwriting external changes

                    // Let's remove the cached file to remove a DB that is in an invalid state
                    cleanCached()

                    // And let's rerun the transaction again.
                    return doTransaction(block)
                }

                // Copy the now edited cached file back to its rightful place
                cachedDb.file.copyToExternal(externalDb.uri).onError {
                    // Let's clean the cached DB as it contains successful changes that are not valid
                    // (because they couldn't be saved to external storage)
                    cleanCached()
                    return it
                }

                // We get the new last modified time and store it in the app DB
                // This optimizes the case where the underlying file didn't change between subsequent calls to transaction
                updateCachedLastModified()
            }
        }

        return dbResult.map { it.result }
    }

    /**
     * Sets the last modified field in the app data to the current last modified date of the external file.
     */
    private fun updateCachedLastModified(externalLastModified: Instant? = externalDb.lastModifiedInstant()) {
        Log.d(LOG_TAG, "New last modified date of DB: $externalLastModified")
        appData.userQueries.setCachedDbLastModified(cachedDbLastModified = externalLastModified, id = userId)
    }

    /**
     * Returns true if the locally cached file is the same as the external one.
     *
     * Uses [DocumentFile.lastModified] to compare them. If this information is not available it will always return `false`.
     */
    private fun isCachedDatabaseUpToDate(externalLastModified: Instant?): Boolean? {
        if (!cachedDb.file.exists()) {
            Log.d(LOG_TAG, "Cached DB does not exist")
            return false
        }

        val lastModifiedResult = appData.userQueries.getCachedDbLastModified(id = userId).executeAsOneOrNull()
        if (lastModifiedResult == null) {
            Log.w(
                LOG_TAG,
                "Unable to obtain cached DB last modified from database. The user was removed while the transaction was running?",
            )
            return null
        }
        val cachedLastModified = lastModifiedResult.cachedDbLastModified

        Log.d(LOG_TAG, "Cached DB last modified = $cachedLastModified - External DB last modified = $externalLastModified")

        // To err on the side of caution, we only return true if the last modified date is EXACTLY the same
        return cachedLastModified != null && externalLastModified != null && cachedLastModified == externalLastModified
    }

    /**
     * Cleans the locally cached version of the DB
     */
    fun cleanCached() {
        val file = cachedDb.file
        if (file.exists()) {
            Log.d(LOG_TAG, "Cleaning cached DB $file")
            if (!file.delete()) {
                Log.e(LOG_TAG, "Unable to delete cached database $file")
            }
        }
    }

    /**
     * This will copy the external DB to the internal storage and start managing internally.
     * The app will have full and sole ownership of the DB.
     *
     * After this function returns [UserDbResult.Completed.Success], this class mustn't be used anymore.
     */
    suspend fun moveToManagedDb(context: Context, coroutineContext: CoroutineContext = Dispatchers.IO): UserDbResult<Unit> {
        return withContext(coroutineContext) {
            // Copies the external DB to internal storage
            val internalDb = UserDbUtils.getManagedDbNameForUser(context, userId)
            externalDb.uri.copyToInternal(internalDb.file).onError { return@withContext it }

            // Removes external DB information from the app DB
            appData.userQueries.setExternalFileUri(externalFileUri = null, cachedDbLastModified = null, id = userId)

            // Removes any cached copy of the DB
            cleanCached()

            // Returns success
            UserDbResult.Completed.Success(Unit)
        }
    }

    /**
     * Removes the locally cached file (if any) and releases the persistent URI permissions for the external DB
     */
    override suspend fun unlink() {
        cleanCached()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.applicationContext.contentResolver.releasePersistableUriPermission(externalDb.uri, flags)
        } catch (e: SecurityException) {
            Log.w(LOG_TAG, "Unable to release persistable URI permissions", e)
        }
    }

    companion object : KoinComponent {

        /**
         * Creates a new [ExternalUserDb] for the given data.
         *
         * Fails if the user database is not currently being managed externally.
         */
        fun of(userId: Long, externalFileUri: URI): ExternalUserDb {
            val context = get<Context>()

            return ExternalUserDb(
                userId = userId,
                cachedDb = UserDbUtils.getCachedDbNameForUser(context, userId),
                externalDb = externalFileUri.toDocumentFile(context),
            )
        }
    }
}
