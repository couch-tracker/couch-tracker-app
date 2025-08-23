package io.github.couchtracker.utils.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Base class to implement a setting.
 *
 * @param DS the type of the underlying [DataStore]
 * @param K the type of the [Preferences.Key]. Due to Kotlin type-system limitations, this should be bound by [Any], but can't.
 * Avoid putting nullables types here, as `null` signifies that the settings is not set.
 * @param V the type of the value, after being parsed. Due to Kotlin type-system limitations, this should be bound by [Any], but can't.
 * Avoid putting nullables types here, as `null` signifies that the settings is not set.
 * @param D the type of the default value. This type widens [V], so that the default type can be nullable.
 */
abstract class Setting<DS, K, V : D, D> {

    abstract val dataStore: DataStore<DS>

    /**
     * The preference key
     */
    abstract val key: Preferences.Key<K>

    /**
     * A [Flow] emitting default values.
     */
    abstract val default: Flow<D>

    /**
     * A [Flow] emitting non-parsed values of type [K]. If emitting `null`, it means the setting is not set.
     */
    protected abstract val actualValue: Flow<K?>

    /**
     * A [Flow] emitting the actual setting's value, without considering [default].
     * It will emit `null` if the setting is not set.
     */
    @UseLoadedSetting
    val actual: Flow<V?>
        get() = actualValue.map { value -> value?.let { parse(it) } }

    /**
     * A [Flow] emitting a [LoadedSetting] instance.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    @UseLoadedSetting
    val currentLoadedSetting: Flow<LoadedSetting<V, D>> by lazy {
        actual.flatMapLatest { value ->
            if (value != null) {
                flowOf(LoadedSetting.Value(setting = this, actual = value))
            } else {
                default.map { LoadedSetting.Default(setting = this, default = it) }
            }
        }
    }

    /**
     * A [Flow] emitting the [actual] if it exists, or [default].
     */
    @UseLoadedSetting
    val current: Flow<D> by lazy {
        currentLoadedSetting.map { it.current }
    }

    /**
     * Parses the base [value] of type [K] into a complex value of type [V].
     * Never called if the setting is not set (i.e. value is `null`).
     */
    protected abstract fun parse(value: K): V

    /**
     * Serializes the complex [value] of type [V] into a base value of type [K].
     * Never called if the setting is not set (i.e. value is `null`).
     */
    protected abstract fun serialize(value: V): K

    /**
     * Commits the base [value] of type [K] into the [DataStore].
     */
    protected abstract suspend fun setValue(value: K)

    /**
     * Sets the given [value] in the backing [dataStore]. [value] will be emitted by [current] and [currentLoadedSetting].
     */
    suspend fun set(value: V) {
        setValue(serialize(value))
    }

    /**
     * Removes this setting from the backing [dataStore]. [default] will be emitted by [current] and [currentLoadedSetting].
     */
    abstract suspend fun reset()
}
