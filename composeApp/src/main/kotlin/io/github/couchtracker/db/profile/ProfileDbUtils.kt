package io.github.couchtracker.db.profile

import android.content.Context
import io.github.couchtracker.db.common.DbPath
import org.koin.core.component.KoinComponent

object ProfileDbUtils : KoinComponent {
    const val MIME_TYPE = "application/x-sqlite3"

    /**
     * Returns the path for a profile database that is managed internally by the app.
     */
    fun getManagedDbNameForProfile(context: Context, profileId: Long): DbPath {
        return DbPath.appDatabase(context, "$profileId.db")
    }

    /**
     * Returns the path for the cached location of a profile database that is managed externally.
     */
    fun getCachedDbNameForProfile(context: Context, profileId: Long): DbPath {
        return DbPath.appCache(context, "$profileId.cached.db")
    }
}
