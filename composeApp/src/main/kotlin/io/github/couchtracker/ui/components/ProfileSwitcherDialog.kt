package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfilesContext
import io.github.couchtracker.R
import io.github.couchtracker.Settings
import io.github.couchtracker.db.app.ProfileInfo
import io.github.couchtracker.intl.datetime.Skeletons
import io.github.couchtracker.intl.datetime.TimeSkeleton
import io.github.couchtracker.intl.datetime.sum
import io.github.couchtracker.intl.formatDateTimeSkeleton
import io.github.couchtracker.ui.screens.settings.ProfilesSettingsScreen
import io.github.couchtracker.utils.str
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun ProfileSwitcherDialog(
    close: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val profilesInfo = LocalProfilesContext.current
    val profiles = profilesInfo.profiles.values.toList()
    val scrollState = rememberLazyListState()

    BasicAlertDialog(onDismissRequest = close) {
        Surface(
            shape = AlertDialogDefaults.shape,
            color = AlertDialogDefaults.containerColor,
            tonalElevation = AlertDialogDefaults.TonalElevation,
        ) {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Icon(
                    imageVector = Icons.Default.Group,
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 8.dp),
                )
                Text(
                    text = R.string.switch_profile.str(),
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp),
                    style = MaterialTheme.typography.headlineSmall,
                )
                LazyColumn(state = scrollState) {
                    items(profiles, key = { it.profile.id }) { profileInfo ->
                        ListItem(
                            headlineContent = { Text(profileInfo.profile.name) },
                            supportingContent = { Text(profileInfo.supportingText()) },
                            modifier = Modifier.clickable {
                                coroutineScope.launch {
                                    Settings.CurrentProfileId.set(profileInfo.profile.id)
                                    close()
                                }
                            },
                            colors = ListItemDefaults.colors(
                                containerColor = if (profilesInfo.current.profile == profileInfo.profile) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    Color.Transparent
                                },
                            ),
                        )
                    }
                    item(key = "divider") {
                        HorizontalDivider()
                    }
                    item(key = "settings") {
                        ListItem(
                            headlineContent = { Text(R.string.profiles_settings.str()) },
                            leadingContent = { Icon(Icons.Filled.ManageAccounts, contentDescription = null) },
                            modifier = Modifier.clickable {
                                navController.navigate(ProfilesSettingsScreen)
                                close()
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfo.supportingText(): String {
    return R.string.profile_last_used_with_date.str(formattedLastModified())
}

@Composable
fun ProfileInfo.formattedLastModified(): String {
    return when (val lastModified = db.lastModified()) {
        null -> R.string.profile_last_used_unknown.str()
        else -> formatDateTimeSkeleton(
            instant = lastModified,
            timeZone = TimeZone.currentSystemDefault(),
            skeleton = (Skeletons.MEDIUM_DATE + TimeSkeleton.SECONDS).sum(),
            locale = Locale.current,
        )
    }
}
