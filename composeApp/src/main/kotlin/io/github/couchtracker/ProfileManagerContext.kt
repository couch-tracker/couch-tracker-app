package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.Profile
import io.github.couchtracker.db.app.ProfileManager
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import org.koin.compose.koinInject

val LocalProfileManagerContext = staticCompositionLocalOf<ProfileManager> { error("no default profile context") }

@Composable
fun ProfileManagerContext(content: @Composable () -> Unit) {
    val appDb = koinInject<AppData>()

    val profilesState = remember { appDb.profileQueries.selectAll() }.asListState()
    val profiles = profilesState.value

    LoadableScreen(
        data = when (profiles.isNullOrEmpty()) {
            true -> Loadable.Loading
            false -> Result.Value(profiles)
        },
    ) { profiles ->
        ProfileManagerContext(profiles = profiles, content = content)
    }
}

@Composable
private fun ProfileManagerContext(profiles: List<Profile>, content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager(profiles, coroutineScope) }
    LaunchedEffect(profiles) {
        profileManager.updateListOfProfiles(profiles)
    }

    CompositionLocalProvider(LocalProfileManagerContext provides profileManager) {
        content()
    }
}
