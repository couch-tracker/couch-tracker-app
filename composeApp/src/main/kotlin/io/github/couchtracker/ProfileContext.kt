package io.github.couchtracker

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.staticCompositionLocalOf
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.Profile
import io.github.couchtracker.db.app.ProfileManager
import org.koin.compose.koinInject

val LocalProfileManagerContext = staticCompositionLocalOf<ProfileManager> { error("no default profile context") }

@Composable
fun ProfileContext(content: @Composable () -> Unit) {
    val appDb = koinInject<AppData>()

    val profilesState = remember { appDb.profileQueries.selectAll() }.asListState()
    val profiles = profilesState.value

    if (profiles.isNullOrEmpty()) {
        Text("Loading...") // TODO do better
    } else {
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
        content()
    }
}
