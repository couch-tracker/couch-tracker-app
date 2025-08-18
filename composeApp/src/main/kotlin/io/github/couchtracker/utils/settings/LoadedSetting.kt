package io.github.couchtracker.utils.settings

import kotlin.reflect.KProperty

/**
 * Stores the values of a loaded setting.
 *
 * It allows to differentiate between a setting with a default value and an "actual" one.
 *
 * To get an instance of this class, use [LoadedSettings.get].
 *
 * The [V] and [D] type bounds are the same as described in [Setting].
 */
sealed interface LoadedSetting<V : D, D> {

    val setting: Setting<*, *, V, D>

    val actual: V?

    val current: D

    data class Value<V : D, D>(
        override val setting: Setting<*, *, V, D>,
        override val actual: V,
    ) : LoadedSetting<V, D> {
        override val current get() = actual
    }

    data class Default<V : D, D>(
        override val setting: Setting<*, *, V, D>,
        val default: D,
    ) : LoadedSetting<V, D> {
        override val actual get() = null
        override val current get() = default
    }

    operator fun getValue(thisObj: Any?, property: KProperty<*>): D {
        return current
    }
}

inline fun <reified V : D, reified D> LoadedSetting<*, *>.cast(setting: Setting<*, *, V, D>): LoadedSetting<V, D> {
    require(this.setting === setting) {
        "The setting given to .cast() must be the same instance as the one that is stored in this LoadedSetting"
    }
    return when (this) {
        is LoadedSetting.Value<*, *> -> LoadedSetting.Value(setting = setting, actual = actual as V)
        is LoadedSetting.Default<*, *> -> LoadedSetting.Default(setting = setting, default = default as D)
    }
}
