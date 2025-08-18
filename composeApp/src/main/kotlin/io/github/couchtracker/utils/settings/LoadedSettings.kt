package io.github.couchtracker.utils.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.Preferences
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.collectAsLoadableWithLifecycle
import io.github.couchtracker.utils.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

private typealias SettingGetter<S, V, D> = S.() -> Setting<*, *, V, D>

/**
 * Class that stores all the loaded settings for a particular [AbstractSettings] instance.
 */
class LoadedSettings<S : AbstractSettings>(
    val settings: S,
    val values: Map<Preferences.Key<*>, LoadedSetting<*, *>>,
) {

    @PublishedApi
    internal inline fun <V : D, D> getSetting(setting: SettingGetter<S, V, D>): Setting<*, *, V, D> {
        val setting = setting(this.settings)
        require(setting.key in values.keys) {
            "Given setting with key ${setting.key} not loaded in this LoadedSettings instance"
        }
        return setting
    }

    inline fun <reified V : D, reified D> get(setting: SettingGetter<S, V, D>): LoadedSetting<V, D> {
        val setting = getSetting(setting)
        return values.getValue(setting.key).cast(setting)
    }

    @Composable
    inline fun <reified V : D, reified D> getWithDefault(setting: S.() -> Setting<*, *, V, D>): Loadable<LoadedSettingWithDefault<V, D>> {
        val setting = getSetting(setting)
        val loaded = values.getValue(setting.key).cast<V, D>(setting)
        val default by setting.default.collectAsLoadableWithLifecycle()

        return default.map {
            LoadedSettingWithDefault(
                loaded = loaded,
                default = it,
            )
        }
    }
}

fun <S : AbstractSettings> S.loaded(): Flow<LoadedSettings<S>> {
    val flowsMap = settings.mapValues { it.value.currentLoadedSetting }

    return combine(flowsMap.values) { loadedSettings ->
        LoadedSettings(
            settings = this@loaded,
            values = flowsMap.keys.zip(loadedSettings).toMap(),
        )
    }
}
