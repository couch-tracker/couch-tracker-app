package io.github.couchtracker.db.app

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import io.github.couchtracker.db.common.InstantColumnAdapter
import io.github.couchtracker.db.common.UriColumnAdapter
import io.requery.android.database.sqlite.RequerySQLiteOpenHelperFactory

object AppDb {

    fun get(context: Context): AppData {
        val driver = AndroidSqliteDriver(
            AppData.Schema,
            context,
            "app.db",
            factory = RequerySQLiteOpenHelperFactory(),
        )
        return AppData(
            driver = driver,
            UserAdapter = User.Adapter(
                externalFileUriAdapter = UriColumnAdapter,
                cachedDbLastModifiedAdapter = InstantColumnAdapter,
            ),
        )
    }
}
