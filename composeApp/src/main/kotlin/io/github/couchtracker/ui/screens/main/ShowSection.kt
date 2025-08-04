@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.utils.str

@Composable
fun ShowSection(innerPadding: PaddingValues) {
    val navController = LocalNavController.current
    val pagerState = rememberPagerState(initialPage = ShowTab.UP_NEXT.ordinal) { ShowTab.entries.size }

    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        backgroundImage = painterResource(R.drawable.sunset),
        actions = {
            MainSectionDefaults.SearchButton(
                onOpenSearch = {
                    navController.navigate(SearchScreen(filter = SearchableMediaType.SHOW))
                },
            )
        },
        tabText = { page -> Text(text = ShowTab.entries[page].displayName.str()) },
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (ShowTab.entries[page]) {
                    ShowTab.HISTORY -> WipMessageComposable(gitHubIssueId = 126)
                    ShowTab.WATCHLIST -> WipMessageComposable(gitHubIssueId = 130)
                    ShowTab.UP_NEXT -> WipMessageComposable(gitHubIssueId = 127)
                    ShowTab.EXPLORE -> WipMessageComposable(gitHubIssueId = 128)
                    ShowTab.CALENDAR -> WipMessageComposable(gitHubIssueId = 129)
                }
            }
        },
    )
}

private enum class ShowTab(
    @StringRes
    val displayName: Int,
) {
    HISTORY(R.string.tab_shows_history),
    WATCHLIST(R.string.tab_shows_watchlist),
    UP_NEXT(R.string.tab_shows_up_next),
    CALENDAR(R.string.tab_shows_calendar),
    EXPLORE(R.string.tab_shows_explore),
}
