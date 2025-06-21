package io.github.couchtracker.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class Setting<K, V : D, D>(
    private val dataStore: Context.() -> DataStore<Preferences>,
    private val key: Preferences.Key<K>,
    private val defaultValue: D,
    private val serialize: (K) -> V,
    private val parse: (V) -> K,
) : KoinComponent, Flow<D> {

    private val context by inject<Context>()

    private val flow: Flow<D> = context.dataStore().data.map { preferences ->
        preferences[key]?.let { serialize(it) } ?: defaultValue
    }

    override suspend fun collect(collector: FlowCollector<D>) = flow.collect(collector)

    /**
     * Sets the given [value] in the settings.
     */
    suspend fun set(value: V) {
        context.dataStore().edit { preferences ->
            preferences[key] = parse(value)
        }
    }

    /**
     * Removes this setting from the [DataStore]. The [defaultValue] will be emitted by [flow].
     */
    suspend fun reset() {
        context.dataStore().edit { preferences ->
            preferences.remove(key)
        }
    }

    companion object {
        operator fun <V : D, D> invoke(
            dataStore: Context.() -> DataStore<Preferences>,
            key: Preferences.Key<V>,
            defaultValue: D,
        ): Setting<V, V, D> = Setting(
            dataStore = dataStore,
            key = key,
            defaultValue = defaultValue,
            serialize = { it },
            parse = { it },
        )
    }
}
