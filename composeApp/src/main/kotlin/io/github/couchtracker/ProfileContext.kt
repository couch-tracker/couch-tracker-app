package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.Profile
import io.github.couchtracker.db.app.ProfileManager
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.Loadable
import org.koin.compose.koinInject

val LocalProfileManagerContext = staticCompositionLocalOf<ProfileManager> { error("no default profile context") }
val LocalFullProfileDataContext = staticCompositionLocalOf<FullProfileData> { error("no default profile context") }

@Composable
fun ProfileContext(content: @Composable () -> Unit) {
    val appDb = koinInject<AppData>()

    val profilesState = remember { appDb.profileQueries.selectAll() }.asListState()
    val profiles = profilesState.value

    LoadableScreen(
        data = when (profiles.isNullOrEmpty()) {
            true -> Loadable.Loading
            false -> Loadable.Loaded(profiles)
        },
    ) { profiles ->
        ProfileContext(profiles = profiles, content = content)
    }
}

@Composable
private fun ProfileContext(profiles: List<Profile>, content: @Composable () -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val profileManager = remember { ProfileManager(profiles, coroutineScope) }
    LaunchedEffect(profiles) {
        profileManager.updateListOfProfiles(profiles)
    }

    CompositionLocalProvider(LocalProfileManagerContext provides profileManager) {
        ProfileWithDataContext(content)
    }
}

@Composable
private fun ProfileWithDataContext(content: @Composable () -> Unit) {
    val profileManager = LocalProfileManagerContext.current
    val fullProfileDataState by profileManager.current.fullProfileDataState.collectAsStateWithLifecycle(initialValue = Loadable.Loading)

    LoadableScreen(
        data = fullProfileDataState,
        onError = { DefaultErrorScreen("Error loading profile data") }, // TODO better error
    ) { fullProfileData ->
        CompositionLocalProvider(LocalFullProfileDataContext provides fullProfileData) {
            content()
        }
    }
}
