package io.github.couchtracker.utils.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.flow.Flow

/**
 * Specialization of [AbstractSettings] that holds [PreferencesSetting].
 *
 * It defines a [dataStore] that all settings created with the [setting] functions will use.
 */
abstract class AbstractPreferencesSettings : AbstractSettings() {

    abstract val dataStore: DataStore<Preferences>

    protected fun <K, V : D, D> setting(
        key: Preferences.Key<K>,
        default: Flow<D>,
        parse: (K) -> V,
        serialize: (V) -> K,
    ) = setting(
        PreferencesSetting(
            dataStore = dataStore,
            key = key,
            default = default,
            parse = parse,
            serialize = serialize,
        ),
    )

    protected fun <V : D, D> setting(
        key: Preferences.Key<V>,
        default: Flow<D>,
    ) = setting(
        key = key,
        default = default,
        parse = { it },
        serialize = { it },
    )
}
