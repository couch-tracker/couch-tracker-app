package io.github.couchtracker.db.user

import android.content.Context
import io.github.couchtracker.db.app.User
import io.github.couchtracker.db.common.DbPath
import org.koin.core.component.KoinComponent

object UserDbUtils : KoinComponent {
    const val MIME_TYPE = "application/x-sqlite3"

    /**
     * Returns the path for a user database that is managed internally by the app.
     */
    fun getManagedDbNameForUser(context: Context, userId: Long): DbPath {
        return DbPath.of(context, "$userId.db")
    }

    /**
     * Returns the path for the cached location of a user database that is managed externally.
     */
    fun getCachedDbNameForUser(context: Context, userId: Long): DbPath {
        return DbPath.of(context, "$userId.cached.db")
    }
}

/**
 * Returns the appropriate instance of [UserDb], depending on whether the DB is managed or external.
 *
 * @see ManagedUserDb
 * @see ExternalUserDb
 */
fun User.db(): UserDb {
    return when (externalFileUri) {
        null -> ManagedUserDb(user = this)
        else -> ExternalUserDb.of(user = this)
    }
}
