@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.db.profile.show.TmdbExternalShowId
import io.github.couchtracker.db.profile.show.UnknownExternalShowId
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.ResultScreen
import io.github.couchtracker.ui.components.SeasonListItem
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.ui.screens.season.navigateToSeason
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultErrorOrNull
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

private const val LOG_TAG = "ShowScreen"

@Serializable
data class ShowScreen(val showId: String) : Screen() {
    @Composable
    override fun content() {
        val externalShowId = ExternalShowId.parse(this@ShowScreen.showId)
        val showId = when (externalShowId) {
            is TmdbExternalShowId -> {
                externalShowId.id
            }
            is UnknownExternalShowId -> TODO()
        }

        Content(
            viewModel {
                ShowScreenViewModel(
                    application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                    externalShowId = externalShowId,
                    showId = showId,
                )
            },
        )
    }
}

fun NavController.navigateToShow(id: TmdbShowId, preloadData: BaseTmdbShow?) {
    if (preloadData != null) {
        KoinPlatform.getKoin().get<TmdbBaseMemoryCache>().registerItem(preloadData)
    }
    navigate(ShowScreen(id.toExternalId().serialize()))
}

private enum class ShowScreenTab {
    VIEWING_HISTORY,
    SEASONS,
    DETAILS,
}

@Composable
private fun Content(viewModel: ShowScreenViewModel) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        ResultScreen(
            error = viewModel.baseDetails.resultErrorOrNull(),
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = { viewModel.retryAll() },
                    )
                }
            },
        ) {
            ShowScreenContent(
                viewModel = viewModel,
                totalHeight = constraints.maxHeight,
                reloadShow = { viewModel.retryAll() },
            )
        }
    }
}

@Composable
private fun ShowScreenContent(
    viewModel: ShowScreenViewModel,
    totalHeight: Int,
    reloadShow: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = ShowScreenTab.entries.indexOf(ShowScreenTab.DETAILS),
        pageCount = { ShowScreenTab.entries.size },
    )
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        loadable = { viewModel.allLoadables },
        onRetry = reloadShow,
    )
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    logCompositions(LOG_TAG, "Recomposing ShowScreenContent")
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = backgroundColor,
            topBar = {
                OverviewScreenComponents.Header(
                    title = viewModel.baseDetails.resultValueOrNull()?.name.orEmpty(),
                    backdrop = viewModel.baseDetails.resultValueOrNull()?.backdrop,
                    scrollBehavior = scrollBehavior,
                    backgroundColor = { backgroundColor },
                    belowAppBar = {
                        OverviewScreenComponents.HeaderTabRow(
                            pagerState = pagerState,
                            tabText = { page ->
                                when (ShowScreenTab.entries[page]) {
                                    ShowScreenTab.VIEWING_HISTORY -> R.string.tab_show_viewing_history.str()
                                    ShowScreenTab.SEASONS -> R.string.tab_show_seasons.str()
                                    ShowScreenTab.DETAILS -> R.string.tab_show_details.str()
                                }
                            },
                            onPageClick = { page ->
                                coroutineScope.launch { pagerState.animateScrollToPage(page) }
                            },
                        )
                    },
                )
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            content = { innerPadding ->
                HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { page ->
                    when (ShowScreenTab.entries[page]) {
                        ShowScreenTab.DETAILS -> OverviewScreenComponents.ShowDetailsContent(
                            innerPadding = innerPadding,
                            viewModel = viewModel,
                            totalHeight = totalHeight,
                        )
                        ShowScreenTab.SEASONS -> OverviewScreenComponents.SeasonsContent(
                            innerPadding = innerPadding,
                            viewModel = viewModel,
                        )
                        ShowScreenTab.VIEWING_HISTORY -> WipMessageComposable(gitHubIssueId = 131)
                    }
                }
            },
        )
    }
}

@Composable
private fun OverviewScreenComponents.ShowDetailsContent(
    innerPadding: PaddingValues,
    viewModel: ShowScreenViewModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val baseDetails = viewModel.baseDetails
    val fullDetails = viewModel.fullDetails
    val images = viewModel.images
    val credits = viewModel.credits
    ContentList(innerPadding, modifier) {
        topSpace()
        section(title = { textBlock("creators", fullDetails.mapResult { it.createdByString }, placeholderLines = 2) }) {
            tags(
                tags = fullDetails.mapResult { details ->
                    listOfNotNull(
                        details.baseDetails.year?.toString(),
                        details.rating?.formatted,
                    ) + details.genres.map { it.name }
                },
            )
        }
        section(title = { tagline(fullDetails.mapResult { it.tagline }) }) {
            overview(baseDetails.mapResult { it.overview })
        }
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.mapResult { it.cast }, totalHeight = totalHeight)
        crewSection(credits.mapResult { it.crew }, totalHeight = totalHeight)
    }
}

@Composable
private fun OverviewScreenComponents.SeasonsContent(
    innerPadding: PaddingValues,
    viewModel: ShowScreenViewModel,
    modifier: Modifier = Modifier,
) {
    val seasons = viewModel.fullDetails.mapResult { it.seasons }
    val navController = LocalNavController.current
    LoadableScreen(
        seasons,
        onError = { exception ->
            DefaultErrorScreen(
                errorMessage = exception.title.string(),
                errorDetails = exception.details?.string(),
                retry = { viewModel.retryAll() },
            )
        },
    ) { seasons ->
        ContentList(
            innerPadding.plus(PaddingValues(vertical = 16.dp, horizontal = 8.dp)),
            modifier,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            items(seasons.size) { index ->
                val (id, season) = seasons[index]
                SeasonListItem(
                    season,
                    onClick = { navController.navigateToSeason(id) },
                    isFirstInList = index == 0,
                    isLastInList = index == seasons.size - 1,
                )
            }
        }
    }
}
