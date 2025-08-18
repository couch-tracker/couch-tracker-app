package io.github.couchtracker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.github.couchtracker.AppSettingsDataStore
import io.github.couchtracker.utils.settings.AbstractPreferencesSettings
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.qualifier.named
import kotlin.getValue

/**
 * Specialization of [AbstractPreferencesSettings] that uses the [AppSettingsDataStore] bound in Koin as [DataStore].
 */
abstract class AbstractAppSettings : AbstractPreferencesSettings(), KoinComponent {
    override val dataStore by inject<DataStore<Preferences>>(named<AppSettingsDataStore>())
}
