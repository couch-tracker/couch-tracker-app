package io.github.couchtracker

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.couchtracker.utils.Setting

private val Context.appSettings: DataStore<Preferences> by preferencesDataStore(name = "settings")

object Settings {
    val CurrentProfileId = Setting(
        dataStore = { appSettings },
        key = longPreferencesKey("currentProfileId"),
        defaultValue = null,
    )
}
