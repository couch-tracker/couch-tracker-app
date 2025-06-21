package io.github.couchtracker.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.R
import io.github.couchtracker.db.app.AppData
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.supportingText
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.toJavaUri
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.FooterPreference
import me.zhanghai.compose.preference.Preference
import org.koin.compose.koinInject

@Serializable
data object ProfilesSettingsScreen : Screen() {

    override fun profileDataContext() = false

    @Composable
    override fun content() = Content()
}

@Composable
private fun Content() {
    val appDb = koinInject<AppData>()
    val navController = LocalNavController.current
    val profileManager = LocalProfileManagerContext.current
    val appContext = LocalContext.current.applicationContext

    val openDbWorkflow = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val document = uri?.let { DocumentFile.fromSingleUri(appContext, uri) }
        if (document != null) {
            val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            appContext.contentResolver.takePersistableUriPermission(uri, takeFlags)

            appDb.profileQueries.insert(
                name = document.name ?: document.uri.toString(),
                externalFileUri = document.uri.toJavaUri(),
            )
        }
    }

    BaseSettings(R.string.profiles.str()) {
        items(profileManager.profiles, key = { it.profile.id }) { profileInfo ->
            Preference(
                modifier = Modifier.animateItem(),
                title = { Text(profileInfo.profile.name) },
                summary = { Text(profileInfo.supportingText()) },
                onClick = { navController.navigate(ProfileSettingsScreen(id = profileInfo.profile.id)) },
            )
        }

        item("divider") { HorizontalDivider(modifier = Modifier.animateItem()) }
        item("create") { CreateProfilePreference(modifier = Modifier.animateItem()) }

        item("open") {
            Preference(
                modifier = Modifier.animateItem(),
                title = { Text(R.string.open_profile.str()) },
                icon = { Icon(Icons.Outlined.FileOpen, contentDescription = null) },
                summary = { Text(R.string.open_profile_summary.str()) },
                onClick = {
                    openDbWorkflow.launch(arrayOf("*/*"))
                },
            )
        }

        item("footer") {
            FooterPreference(
                modifier = Modifier.animateItem(),
                summary = { Text(R.string.profiles_footer.str()) },
            )
        }
    }
}

@Composable
private fun CreateProfilePreference(modifier: Modifier) {
    val appDb = koinInject<AppData>()
    var dialogOpen by remember { mutableStateOf(false) }

    Preference(
        modifier = modifier,
        title = { Text(R.string.create_profile.str()) },
        icon = { Icon(Icons.Default.PersonAdd, contentDescription = null) },
        summary = { Text(R.string.create_profile_summary.str()) },
        onClick = { dialogOpen = true },
    )

    if (dialogOpen) {
        ProfileNameDialog(
            initialName = "",
            onOk = { appDb.profileQueries.insert(name = it, externalFileUri = null) },
            onDismissRequest = { dialogOpen = false },
        )
    }
}
