@file:OptIn(ExperimentalFoundationApi::class)

package io.github.couchtracker.ui.screens.main

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.cachedIn
import androidx.paging.compose.collectAsLazyPagingItems
import app.moviebase.tmdb.model.TmdbTimeWindow
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalShowId
import io.github.couchtracker.settings.AppSettings
import io.github.couchtracker.tmdb.tmdbPager
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.ShowPortrait
import io.github.couchtracker.ui.components.ShowPortraitModel
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.components.toShowPortraitModels
import io.github.couchtracker.ui.screens.main.ShowSectionViewModel.BookmarkedShowData
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.error.CouchTrackerResult
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.removeDuplicates
import io.github.couchtracker.utils.settings.get
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

@Composable
fun ShowSection(
    innerPadding: PaddingValues,
    viewModel: ShowSectionViewModel = viewModel(),
) {
    // TODO: open up next as a first tab
    val pagerState = rememberPagerState(initialPage = ShowTab.WATCHLIST.ordinal) { ShowTab.entries.size }
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        errors = { viewModel.allErrors },
        onRetry = { viewModel.retryAll() },
    )
    MainSection(
        innerPadding = innerPadding,
        pagerState = pagerState,
        imageModel = R.drawable.sunset,
        title = R.string.main_section_shows.str(),
        actions = {
            MainSectionDefaults.DefaultAppBarActions()
        },
        tabText = { page -> Text(text = ShowTab.entries[page].displayName.str()) },
        snackbarHostState = snackbarHostState,
        page = { page ->
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (ShowTab.entries[page]) {
                    ShowTab.HISTORY -> WipMessageComposable(
                        gitHubIssueId = 126,
                        description = "All watched episodes of any show",
                    )
                    ShowTab.WATCHLIST -> BookmarkedShowGrid(
                        shows = viewModel.watchlist,
                        emptyMessage = R.string.tab_shows_watchlist_empty.str(),
                        emptyDescription = R.string.tab_shows_watchlist_empty_description.str(),
                    )
                    ShowTab.FOLLOWING -> BookmarkedShowGrid(
                        shows = viewModel.following,
                        emptyMessage = R.string.tab_shows_following_empty.str(),
                        emptyDescription = R.string.tab_shows_following_empty_description.str(),
                    )
                    ShowTab.UP_NEXT -> WipMessageComposable(
                        gitHubIssueId = 127,
                        description = "" +
                            "- For each active watch session of a bookmarked show, the next unwatched episode\n" +
                            "- For each bookmarked show, without any watch sessions, the pilot",
                    )
                    ShowTab.EXPLORE -> ShowListComposable(viewModel.exploreState)
                    ShowTab.CALENDAR -> WipMessageComposable(
                        gitHubIssueId = 129,
                        description = "A calendar of the episodes of bookmarked shows",
                    )
                }
            }
        },
    )
}

@Composable
private fun BookmarkedShowGrid(
    shows: Loadable<List<Pair<ExternalShowId, CouchTrackerResult<BookmarkedShowData>>>>,
    emptyMessage: String,
    emptyDescription: String,
) {
    LoadableScreen(shows) { shows ->
        if (shows.isEmpty()) {
            MessageComposable(
                modifier = Modifier.fillMaxSize(),
                icon = Icons.Default.BookmarkBorder,
                message = emptyMessage,
                details = emptyDescription,
            )
        } else {
            LazyVerticalGrid(
                modifier = Modifier.fillMaxSize(),
                columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH),
                contentPadding = PaddingValues(8.dp) + PaddingValues(bottom = OverviewScreenComponents.LIST_BOTTOM_SPACE),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(shows) { (showId, bookmarkedShow) ->
                    ShowPortrait(
                        modifier = Modifier.fillMaxWidth(),
                        showId = showId,
                        showResult = bookmarkedShow.map { data ->
                            data.portraitModel.copy(
                                downloadState = when (val seasons = data.seasons) {
                                    is Loadable.Loaded -> when (seasons.value) {
                                        is Result.Error -> ShowPortraitModel.DownloadState.Error
                                        is Result.Value -> ShowPortraitModel.DownloadState.Downloaded
                                    }
                                    Loadable.Loading -> ShowPortraitModel.DownloadState.Loading
                                },
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShowListComposable(
    tabState: ShowExploreTabState,
) {
    val lazyItems = tabState.showFlow.collectAsLazyPagingItems()
    PaginatedGrid(lazyItems, columns = GridCells.Adaptive(minSize = PortraitComposableDefaults.SUGGESTED_WIDTH)) { show, _ ->
        ShowPortrait(Modifier.fillMaxWidth(), show)
    }
}

private enum class ShowTab(
    @StringRes
    val displayName: Int,
) {
    HISTORY(R.string.tab_shows_history),
    WATCHLIST(R.string.tab_shows_watchlist),
    FOLLOWING(R.string.tab_shows_following),
    UP_NEXT(R.string.tab_shows_up_next),
    CALENDAR(R.string.tab_shows_calendar),
    EXPLORE(R.string.tab_shows_explore),
}

class ShowExploreTabState(application: Application, viewModelScope: CoroutineScope) {

    @OptIn(ExperimentalCoroutinesApi::class)
    val showFlow = AppSettings.get { Tmdb.Languages }
        .map { it.current.apiLanguage }
        .distinctUntilChanged()
        .flatMapLatest { tmdbLanguage ->
            tmdbPager(
                downloader = { page ->
                    trending.getTrendingShows(timeWindow = TmdbTimeWindow.DAY, page = page, language = tmdbLanguage.apiParameter)
                },
                mapper = { show ->
                    show.toShowPortraitModels(application, tmdbLanguage)
                },
            ).flow
        }
        .removeDuplicates { it.id }
        .cachedIn(viewModelScope)
}
