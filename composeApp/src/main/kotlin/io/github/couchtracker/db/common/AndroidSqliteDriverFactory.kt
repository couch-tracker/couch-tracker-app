package io.github.couchtracker.db.common

import android.content.Context
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.couchtracker.db.profile.ProfileDb
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory
import io.requery.android.database.sqlite.SQLiteDatabaseConfiguration
import org.koin.core.component.KoinComponent
import org.koin.core.component.get

class AndroidSqliteDriverFactory(
    private val schema: SqlSchema<QueryResult.Value<Unit>>,
) : SqliteDriverFactory, KoinComponent {

    private val appContext = get<Context>()

    override fun getDriver(dbPath: DbPath): SqlDriver {
        return AndroidSqliteDriver(
            schema = schema,
            context = appContext,
            name = dbPath.name,
            factory = RequerySQLiteOpenHelperFactory(
                listOf(
                    RequerySQLiteOpenHelperFactory.ConfigurationOptions { configuration ->
                        SQLiteDatabaseConfiguration(
                            dbPath.file.absolutePath,
                            configuration.openFlags,
                            @Suppress("DEPRECATION")
                            configuration.customFunctions,
                            configuration.functions,
                            configuration.customExtensions,
                        )
                    },
                ),
            ),
            callback = object : AndroidSqliteDriver.Callback(schema) {
                override fun onCorruption(db: SupportSQLiteDatabase) {
                    Log.e(ProfileDb.LOG_TAG, "Database $dbPath is corrupted!")
                    // Do not call super.onCorruption() here!
                    // It will delete the file and later in the process a new empty DB will be created, which is not what we want
                    throw DBCorruptedException()
                }

                override fun onOpen(db: SupportSQLiteDatabase) {
                    db.setForeignKeyConstraintsEnabled(true)
                }
            },
        )
    }
}
