package io.github.couchtracker.db.app

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object AppDb {

    fun get(context: Context): AppData {
        val driver = AndroidSqliteDriver(AppData.Schema, context, "app.db")
        return AppData(driver)
    }
}
