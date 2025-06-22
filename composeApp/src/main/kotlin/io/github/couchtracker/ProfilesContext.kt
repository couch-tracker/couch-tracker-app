package io.github.couchtracker

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cash.sqldelight.coroutines.asFlow
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.db.app.ProfilesInfo
import io.github.couchtracker.db.app.profilesInfoFlow
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import kotlinx.coroutines.flow.map
import org.koin.compose.koinInject

val LocalProfilesContext = compositionLocalOf<ProfilesInfo> { error("no default profiles context") }

@Composable
fun ProfilesContext(content: @Composable () -> Unit) {
    val appDb = koinInject<AppData>()

    val profiles = remember { appDb.profileQueries.selectAll().asFlow().map { it.executeAsList() } }
    val flow = remember {
        profilesInfoFlow(profiles, Settings.CurrentProfileId).map { Result.Value(it) }
    }

    val profilesInfo by flow.collectAsStateWithLifecycle(Loadable.Loading)

    LoadableScreen(profilesInfo) { profilesInfo ->
        CompositionLocalProvider(LocalProfilesContext provides profilesInfo) {
            content()
        }
    }
}
