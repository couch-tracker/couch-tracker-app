package io.github.couchtracker

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.ui.components.CouchTrackerErrorComposable
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.utils.str

val LocalFullProfileDataContext = compositionLocalOf<FullProfileData> { error("no full profile data context") }

@Composable
fun ProfileDataContext(content: @Composable () -> Unit) {
    val profilesInfo = LocalProfilesContext.current

    LoadableScreen(
        data = profilesInfo.currentFullData,
        onError = { ProfileError(it) },
    ) { fullProfileData ->
        CompositionLocalProvider(LocalFullProfileDataContext provides fullProfileData) {
            content()
        }
    }
}

@Composable
private fun ProfileError(error: ProfileDbError) {
    var showProfileDialog by remember { mutableStateOf(false) }

    CouchTrackerErrorComposable(
        modifier = Modifier.fillMaxSize(),
        error = error,
        message = R.string.error_opening_profile.str(),
        afterTitle = {
            Button(onClick = { showProfileDialog = true }) {
                Text(R.string.switch_profile.str())
            }
        },
    )

    if (showProfileDialog) {
        ProfileSwitcherDialog(close = { showProfileDialog = false })
    }
}
