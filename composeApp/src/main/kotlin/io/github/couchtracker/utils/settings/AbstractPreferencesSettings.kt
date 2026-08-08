package io.github.couchtracker.utils.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlin.enums.enumEntries

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

    protected inline fun <reified E : Enum<E>> setting(
        key: String,
        default: E,
    ): PreferencesSetting<String, E, E> {
        val entries = enumEntries<E>()
        return setting(
            key = stringPreferencesKey(key),
            default = flowOf(default),
            serialize = { it.name },
            parse = { entries.single { entry -> entry.name == it } },
        )
    }

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
