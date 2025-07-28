package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.LocalProfilesContext
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.ui.components.ProfilePane
import io.github.couchtracker.ui.components.ProfileSwitcherDialog
import io.github.couchtracker.ui.screens.settings.MainSettingsScreen
import io.github.couchtracker.ui.screens.show.navigateToShow
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
            Column {
                DebugContent()
            }
        },
    )
}

// TODO fix
private enum class HomeTab(@StringRes val displayName: Int) {
    TIMELINE(R.string.tab_shows_timeline),
    FOLLOWED(R.string.tab_shows_followed),
    UP_NEXT(R.string.tab_shows_up_next),
    CALENDAR(R.string.tab_shows_calendar),
    EXPLORE(R.string.tab_shows_explore),
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

private val DEBUG_SHOWS = listOf(
    "Doctor Who" to TmdbShowId(57_243),
    "Fringe" to TmdbShowId(1705),
    "Mario" to TmdbShowId(47_319),
    "The Simpsons" to TmdbShowId(456),
    "How I Met Your Mother" to TmdbShowId(1100),
    "It's Always Sunny in Philadelphia" to TmdbShowId(2710),
    "Watch live" to TmdbShowId(22_980),
)

// TODO: remove this debug composable
@Composable
private fun DebugContent() {
    val profilesInfo = LocalProfilesContext.current
    val navController = LocalNavController.current

    Text(text = "Current profile: ${profilesInfo.current.profile.name}", fontSize = 30.sp)
    ProfilePane()

    Text("Random shows:", style = MaterialTheme.typography.titleMedium)
    for ((showName, showId) in DEBUG_SHOWS) {
        Button(
            onClick = { navController.navigateToShow(TmdbShow(showId, TMDB_LANGUAGE)) },
            content = { Text(showName) },
        )
    }
}
