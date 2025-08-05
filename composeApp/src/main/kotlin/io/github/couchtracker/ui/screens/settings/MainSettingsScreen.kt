package io.github.couchtracker.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.utils.str
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.preference
import me.zhanghai.compose.preference.preferenceCategory

@Serializable
data object MainSettingsScreen : Screen() {
    @Composable
    override fun content() = Content()
}

@Composable
private fun Content() {
    val navController = LocalNavController.current
    BaseSettings(R.string.settings.str(), header = null, footer = null) {
        preferenceCategory(
            key = "app-category",
            title = { Text(R.string.app_settings.str()) },
        )
        preference(
            key = "profiles",
            title = { Text(R.string.profiles.str()) },
            icon = {
                Icon(Icons.Filled.ManageAccounts, contentDescription = null)
            },
            onClick = { navController.navigate(ProfilesSettingsScreen) },
        )
    }
}
