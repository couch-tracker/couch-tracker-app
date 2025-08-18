package io.github.couchtracker.utils.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Implementation of [Setting] that uses a [Preferences] object as its backing data store.
 */
class PreferencesSetting<K, V : D, D>(
    override val dataStore: DataStore<Preferences>,
    override val key: Preferences.Key<K>,
    override val default: Flow<D>,
    private val parse: (K) -> V,
    private val serialize: (V) -> K,
) : Setting<Preferences, K, V, D>() {

    override val actualValue = dataStore.data.map { preferences ->
        preferences[key]
    }

    override fun parse(value: K): V {
        return parse.invoke(value)
    }

    override fun serialize(value: V): K {
        return serialize.invoke(value)
    }

    override suspend fun setValue(value: K) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }

    override suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.remove(key)
        }
    }
}
