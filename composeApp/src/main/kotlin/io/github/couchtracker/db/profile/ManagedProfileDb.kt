package io.github.couchtracker.db.profile

import android.content.Context
import android.content.Intent
import android.util.Log
import io.github.couchtracker.db.common.DbPath
import io.github.couchtracker.db.lastModifiedInstant
import io.github.couchtracker.db.toDocumentFile
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.onError
import io.github.couchtracker.utils.toAndroidUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.core.component.get
import java.io.IOException
import java.net.URI
import kotlin.coroutines.CoroutineContext
import kotlin.io.path.fileSize
import kotlin.io.path.getLastModifiedTime
import kotlin.time.toKotlinInstant

/**
 * A [ProfileDb] that is managed internally by the app.
 *
 * The [transaction] function of this class simply opens the DB, performs the operation and closes it.
 */
class ManagedProfileDb(private val profileId: Long) : ProfileDb() {

    override fun size() = try {
        dbPath(context = get()).file.toPath().fileSize()
    } catch (ignored: IOException) {
        null
    }

    override fun lastModified() = try {
        dbPath(context = get()).file.toPath().getLastModifiedTime().toInstant().toKotlinInstant()
    } catch (ignored: IOException) {
        null
    }

    override suspend fun <T> doTransaction(block: DatabaseTransaction<TransactionResult<T>>): ProfileDbResult<T> {
        val context = get<Context>()
        return openAndUseDatabase(
            dbPath = dbPath(context),
            onCorrupted = {
                Log.e(LOG_TAG, "Internally managed DB is corrupted!")
                // no-op here, we don't want to delete a potentially salvageable file
            },
            block = block,
        ).map { it.result }
    }

    /**
     * This will copy the DB to the external storage in the file located by [uri].
     * The app will start sharing ownership of the DB.
     *
     * After this function returns [Result.Value], this class mustn't be used anymore.
     */
    suspend fun moveToExternalDb(context: Context, uri: URI, coroutineContext: CoroutineContext = Dispatchers.IO): ProfileDbResult<Unit> {
        return withContext(coroutineContext) {
            // Take persistent URI
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(uri.toAndroidUri(), takeFlags)
            val externalDocument = uri.toDocumentFile(context)

            // No-op write, just make sure that a database file exists (so we can copy it)
            val writeResult = ensureDbExists()
            if (writeResult !is Result.Value) {
                Log.e(LOG_TAG, "Unable to write to locally managed DB file! Something's wrong")
                return@withContext writeResult
            }

            // Copy internal file to selected external location
            val managedDbPath = dbPath(context)
            managedDbPath.file.copyToExternal(uri.toAndroidUri()).onError { return@withContext it }

            // Make managed DB a cached of the DB
            managedDbPath.file.renameTo(ProfileDbUtils.getCachedDbNameForProfile(context, profileId).file)

            // Set external URI
            appData.profileQueries.setExternalFileUri(
                id = profileId,
                externalFileUri = uri,
                cachedDbLastModified = externalDocument.lastModifiedInstant(),
            )

            // Return success
            Result.Value(Unit)
        }
    }

    /**
     * Irreversibly deletes the internally managed DB
     */
    override suspend fun unlink() {
        val dbPath = dbPath(context)
        Log.i(LOG_TAG, "Deleting internal DB $this")
        withContext(Dispatchers.IO) {
            if (dbPath.file.exists() && !dbPath.file.delete()) {
                error("Unable to delete internal DB file ${dbPath.file}")
            }
        }
    }

    private fun dbPath(context: Context): DbPath {
        return ProfileDbUtils.getManagedDbNameForProfile(context, profileId)
    }
}
