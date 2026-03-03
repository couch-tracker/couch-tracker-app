package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.collectAsLoadableWithLifecycle
import kotlinx.coroutines.flow.Flow
import org.koin.compose.koinInject

val LocalProfilesContext = compositionLocalOf<ProfilesInfo> { error("no default profiles context") }

@Composable
fun ProfilesContext(content: @Composable () -> Unit) {
    val flow = koinInject<Flow<ProfilesInfo>>()
    val profilesInfo by flow.collectAsLoadableWithLifecycle()

    LoadableScreen(profilesInfo) { profilesInfo ->
        CompositionLocalProvider(LocalProfilesContext provides profilesInfo) {
            content()
        }
    }
}
