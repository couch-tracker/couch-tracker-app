package io.github.couchtracker.utils.settings

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn

/**
 * Creates a [SharedFlow] of [LoadedSettings] from the given [settingsFlow]  in the given [scope].
 */
class LoadedSettingsFlow<S : AbstractSettings>(
    scope: CoroutineScope,
    settingsFlow: Flow<LoadedSettings<S>>,
) {
    val settings: SharedFlow<LoadedSettings<S>> = settingsFlow.shareIn(scope, SharingStarted.Eagerly, replay = 1)
}

/**
 * Interface to be implemented by a class that is able to get a [LoadedSettingsFlow] that eases obtaining the loaded settings from it.
 *
 * See [get] and [getWithDefault].
 */
interface LoadedSettingsGetter<S : AbstractSettings> {
    val loadedSettingsFlow: LoadedSettingsFlow<S>
}

inline fun <S : AbstractSettings, reified V : D, reified D> LoadedSettingsGetter<S>.get(
    crossinline setting: S.() -> Setting<*, *, V, D>,
): Flow<LoadedSetting<V, D>> {
    return loadedSettingsFlow.settings.map { it.get(setting) }
}

@OptIn(ExperimentalCoroutinesApi::class)
inline fun <S : AbstractSettings, reified V : D, reified D> LoadedSettingsGetter<S>.getWithDefault(
    crossinline setting: S.() -> Setting<*, *, V, D>,
): Flow<LoadedSettingWithDefault<V, D>> {
    return loadedSettingsFlow.settings.flatMapConcat { it.getWithDefault(setting) }
}
