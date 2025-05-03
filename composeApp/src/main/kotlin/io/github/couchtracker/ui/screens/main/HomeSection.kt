package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfileManagerContext
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.ProfilePane
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.ui.screens.settings.MainSettingsScreen
import io.github.couchtracker.utils.str

@Composable
fun HomeSection(innerPadding: PaddingValues) {
    val pagerState = rememberPagerState(initialPage = HomeTab.UP_NEXT.ordinal) { HomeTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(R.drawable.sunset),
        tabText = { page -> Text(text = HomeTab.entries[page].displayName.str()) },
        actions = { AppBarIcons() },
        page = { page ->
            Column {
                ProfileSection()
            }
        },
    )
}

// TODO fix
private enum class HomeTab(@StringRes val displayName: Int) {
    TIMELINE(R.string.tab_show_timeline),
    FOLLOWED(R.string.tab_show_followed),
    UP_NEXT(R.string.tab_show_up_next),
    CALENDAR(R.string.tab_show_calendar),
    EXPLORE(R.string.tab_show_explore),
}

@Composable
private fun AppBarIcons() {
    val navController = LocalNavController.current
    var expanded by remember { mutableStateOf(false) }
    var switchProfileDialogOpen by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = !expanded }) {
        Icon(Icons.Default.MoreVert, contentDescription = R.string.more_options.str())
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = { expanded = false },
    ) {
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Group, contentDescription = null) },
            text = { Text(R.string.switch_profile.str()) },
            onClick = {
                expanded = false
                switchProfileDialogOpen = true
            },
        )
        DropdownMenuItem(
            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
            text = { Text(R.string.settings.str()) },
            onClick = {
                expanded = false
                navController.navigate(MainSettingsScreen)
            },
        )
    }
    if (switchProfileDialogOpen) {
        ProfileSwitcherDialog(close = { switchProfileDialogOpen = false })
    }
}

// TODO: remove this debug composable
@Composable
private fun ProfileSection() {
    val profileManager = LocalProfileManagerContext.current

    Text(text = "Current profile: ${profileManager.current.profile.name}", fontSize = 30.sp)
    ProfilePane()
}
