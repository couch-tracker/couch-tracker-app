@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.ui.ImagePreloadOptions
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.ShowPortrait
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.components.toShowPortraitModels
import io.github.couchtracker.ui.screens.show.navigateToShow
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope

class ShowSectionViewModel : ViewModel() {
    val exploreState = ShowExploreTabState(viewModelScope)
}

@Composable
fun ShowSection(
    innerPadding: PaddingValues,
    viewModel: ShowSectionViewModel = viewModel(),
) {
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
                    ShowTab.EXPLORE -> ShowListComposable(viewModel.exploreState)
                    ShowTab.CALENDAR -> WipMessageComposable(gitHubIssueId = 129)
                }
            }
        },
    )
}

@Composable
private fun ShowListComposable(
    tabState: ShowExploreTabState,
) {
    val lazyItems = tabState.showFlow.collectAsLazyPagingItems()
    val navController = LocalNavController.current
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { show ->
        ShowPortrait(Modifier.fillMaxWidth(), show) {
            navController.navigateToShow(it.show)
        }
    }
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

class ShowExploreTabState(viewModelScope: CoroutineScope) {

    private val pager = tmdbPager(
        downloader = { page ->
            trending.getTrendingShows(TmdbTimeWindow.DAY, page = page, TMDB_LANGUAGE.apiParameter)
        },
        mapper = { show ->
            show.toShowPortraitModels(TMDB_LANGUAGE, ImagePreloadOptions.DoNotPreload)
        },
    )
    val showFlow = pager.flow.removeDuplicates { it.show.id.value }.cachedIn(viewModelScope)
}
