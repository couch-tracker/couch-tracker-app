package io.github.couchtracker.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.longPreferencesKey
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.collectAsLoadableWithLifecycle
import io.github.couchtracker.utils.settings.LoadedSettings
import io.github.couchtracker.utils.settings.loaded
import kotlinx.coroutines.flow.flowOf

val LocalAppSettingsContext = compositionLocalOf<LoadedSettings<AppSettings>> { error("no default app settings context") }

object AppSettings : AbstractAppSettings() {

    val Tmdb = settings(TmdbSettings)

    val CurrentProfileId = setting(
        key = longPreferencesKey("currentProfileId"),
        default = flowOf(null),
    )
}

@Composable
fun AppSettingsContext(content: @Composable () -> Unit) {
    val loadedSettings by AppSettings.loaded().collectAsLoadableWithLifecycle()

    LoadableScreen(loadedSettings) { loadedSettings ->
        CompositionLocalProvider(LocalAppSettingsContext provides loadedSettings) {
            content()
        }
    }
}
