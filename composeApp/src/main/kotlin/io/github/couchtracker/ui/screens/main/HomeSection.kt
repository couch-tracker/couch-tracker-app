package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.screens.settings.MainSettingsScreen
import io.github.couchtracker.utils.str

@Composable
fun HomeSection(innerPadding: PaddingValues) {
    val navController = LocalNavController.current
    val pagerState = rememberPagerState(initialPage = HomeTab.UP_NEXT.ordinal) { HomeTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(R.drawable.sunset),
        actions = {
            MainSectionDefaults.SearchButton(
                onOpenSearch = {
                    navController.navigate(SearchScreen(filter = null))
                },
            )
            AppbarMoreMenu()
        },
        tabText = { page -> Text(text = HomeTab.entries[page].displayName.str()) },
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (HomeTab.entries[page]) {
                    HomeTab.HISTORY -> WipMessageComposable(gitHubIssueId = 126)
                    HomeTab.UP_NEXT -> WipMessageComposable(gitHubIssueId = 127)
                    HomeTab.EXPLORE -> WipMessageComposable(gitHubIssueId = 128)
                    HomeTab.CALENDAR -> WipMessageComposable(gitHubIssueId = 129)
                }
            }
        },
    )
}

private enum class HomeTab(@StringRes val displayName: Int) {
    HISTORY(R.string.tab_home_history),
    UP_NEXT(R.string.tab_home_up_next),
    EXPLORE(R.string.tab_home_explore),
    CALENDAR(R.string.tab_home_calendar),
}

@Composable
private fun AppbarMoreMenu() {
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
