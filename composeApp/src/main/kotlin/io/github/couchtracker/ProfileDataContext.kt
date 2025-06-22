package io.github.couchtracker

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.couchtracker.db.profile.FullProfileData
import io.github.couchtracker.db.profile.ProfileDbError
import io.github.couchtracker.intl.errorMessage
import io.github.couchtracker.ui.components.ExceptionStackTrace
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.utils.str

val LocalFullProfileDataContext = staticCompositionLocalOf<FullProfileData> { error("no default profile context") }

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

    MessageComposable(
        modifier = Modifier.fillMaxSize(),
        icon = Icons.Filled.Error,
        message = R.string.error_opening_profile.str(),
    ) {
        Text(error.errorMessage(), textAlign = TextAlign.Center)

        Spacer(Modifier.height(24.dp))
        Button(onClick = { showProfileDialog = true }) {
            Text(R.string.switch_profile.str())
        }
        val exception = if (error is ProfileDbError.WithException) error.exception else null
        if (exception != null) {
            Spacer(Modifier.size(8.dp))
            ExceptionStackTrace(exception, Modifier.heightIn(max = 200.dp))
        }
    }

    if (showProfileDialog) {
        ProfileSwitcherDialog(close = { showProfileDialog = false })
    }
}
