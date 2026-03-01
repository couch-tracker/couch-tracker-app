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
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.screens.settings.MainSettingsScreen
import io.github.couchtracker.utils.str

@Composable
fun ProfileSection(innerPadding: PaddingValues) {
    val navController = LocalNavController.current
    val pagerState = rememberPagerState(initialPage = ProfileTab.LISTS.ordinal) { ProfileTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.sunset,
        title = R.string.main_section_profile.str(),
        actions = {
            MainSectionDefaults.SearchButton(
                onOpenSearch = {
                    navController.navigate(SearchScreen(filter = null))
                },
            )
            AppbarMoreMenu()
        },
        tabText = { page -> Text(text = ProfileTab.entries[page].displayName.str()) },
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (ProfileTab.entries[page]) {
                    ProfileTab.HISTORY -> WipMessageComposable(
                        gitHubIssueId = 126,
                        description = "All watched items",
                    )
                    ProfileTab.LISTS -> {
                        WipMessageComposable(
                            gitHubIssueId = 182,
                            description = "The list of lists",
                        )
                    }
                    ProfileTab.STATS -> WipMessageComposable(
                        gitHubIssueId = 180,
                        description = "Statistics about your profile",
                    )
                }
            }
        },
    )
}

private enum class ProfileTab(@StringRes val displayName: Int) {
    HISTORY(R.string.tab_profile_history),
    LISTS(R.string.tab_profile_lists),
    STATS(R.string.tab_profile_stats),
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
