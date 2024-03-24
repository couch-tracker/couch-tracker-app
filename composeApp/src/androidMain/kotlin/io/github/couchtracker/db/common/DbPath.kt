package io.github.couchtracker.db.common

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import java.io.File

/**
 * Represents a DB the storage location of a database in the app data.
 *
 * @property name the name of the DB, that was/can be used in [SQLiteDatabase.openOrCreateDatabase]
 * @property file the absolute path of the database file.
 *
 * @see Context.getDatabasePath
 */
class DbPath private constructor(val name: String, val file: File) {

    companion object {
        fun of(context: Context, name: String): DbPath {
            return DbPath(name = name, file = context.getDatabasePath(name))
        }
    }
}
