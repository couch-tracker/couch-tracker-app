package io.github.couchtracker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.couchtracker.AppSettingsDataStore
import io.github.couchtracker.utils.settings.AbstractPreferencesSettings
import org.koin.core.qualifier.named
import org.koin.mp.KoinPlatform

/**
 * Specialization of [AbstractPreferencesSettings] that uses the [AppSettingsDataStore] bound in Koin as [DataStore].
 */
abstract class AbstractAppSettings : AbstractPreferencesSettings() {
    override val dataStore by KoinPlatform.getKoin().inject<DataStore<Preferences>>(named<AppSettingsDataStore>())
}
