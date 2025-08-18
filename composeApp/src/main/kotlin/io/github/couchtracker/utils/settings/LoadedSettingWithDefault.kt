package io.github.couchtracker.utils.settings

/**
 * Wrapper around a [LoadedSetting] that also holds the default value [D], regardless of whether its currently being used or not.
 *
 * This class is useful when knowing the default value of a [Setting] in advance is required, for instance when using
 * [rememberWriteThroughAsyncSetting].
 *
 * To get an instance of this class, use [LoadedSettings.getWithDefault].
 *
 * The [V] and [D] type bounds are the same as described in [Setting].
 */
data class LoadedSettingWithDefault<V : D, D>(
    val loaded: LoadedSetting<V, D>,
    val default: D,
) {
    val setting: Setting<*, *, V, D> get() = loaded.setting

    val actual: V? get() = loaded.actual

    val current: D get() = loaded.current
}
