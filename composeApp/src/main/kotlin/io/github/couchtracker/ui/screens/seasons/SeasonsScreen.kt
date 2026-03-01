@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.seasons

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.TmdbExternalSeasonId
import io.github.couchtracker.db.profile.externalids.UnknownExternalSeasonId
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.EpisodeListItem
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.ui.screens.episodes.navigateToEpisode
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val LOG_TAG = "SeasonsScreen"

@Serializable
data class SeasonsScreen(val seasonId: String) : Screen() {
    @Composable
    override fun content() {
        val externalSeasonId = ExternalSeasonId.parse(this@SeasonsScreen.seasonId)
        val seasonId = when (externalSeasonId) {
            is TmdbExternalSeasonId -> externalSeasonId.id
            is UnknownExternalSeasonId -> TODO()
        }

        Content(
            viewModel {
                SeasonsScreenViewModel(
                    application = viewModelApplication(),
                    showId = seasonId.showId,
                )
            },
            initialSeason = externalSeasonId,
        )
    }
}

fun NavController.navigateToSeason(id: ExternalSeasonId) {
    navigate(SeasonsScreen(ExternalSeasonId.serialize(id)))
}

@Composable
private fun Content(viewModel: SeasonsScreenViewModel, initialSeason: ExternalSeasonId) {
    LoadableScreen(
        data = viewModel.showDetails,
        onError = { exception ->
            Surface {
                DefaultErrorScreen(
                    errorMessage = exception.title.string(),
                    errorDetails = exception.details?.string(),
                    retry = { viewModel.retryAll() },
                )
            }
        },
    ) { showDetails ->
        SeasonsScreenContent(
            viewModel = viewModel,
            showDetails = showDetails,
            initialSeason = initialSeason,
            reloadSeason = { viewModel.retryAll() },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SeasonsScreenContent(
    viewModel: SeasonsScreenViewModel,
    initialSeason: ExternalSeasonId,
    showDetails: SeasonsScreenViewModelHelper.ShowDetails,
    reloadSeason: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        loadable = { viewModel.allLoadables },
        onRetry = reloadSeason,
    )
    val pagerState = rememberPagerState(
        initialPage = showDetails.seasons.indexOfFirst { it.externalId == initialSeason },
        pageCount = { showDetails.seasons.size },
    )
    val selectedSeason = showDetails.seasons[pagerState.currentPage]
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    logCompositions(LOG_TAG, "Recomposing SeasonsScreenContent")
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = backgroundColor,
            topBar = {
                OverviewScreenComponents.Header(
                    title = selectedSeason.name ?: selectedSeason.defaultName,
                    subtitle = showDetails.name,
                    backdrop = showDetails.backdrop,
                    scrollBehavior = scrollBehavior,
                    backgroundColor = { backgroundColor },
                    belowAppBar = {
                        OverviewScreenComponents.HeaderScrollableTabRow(
                            pagerState = pagerState,
                            tabText = { page ->
                                showDetails.seasons[page].defaultName
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
                HorizontalPager(pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val seasonDetails = showDetails.seasons[page]
                    OverviewScreenComponents.SeasonPage(
                        innerPadding = innerPadding,
                        viewModel = viewModel,
                        seasonBaseData = seasonDetails,
                    )
                }
            },
        )
    }
}

@Composable
private fun OverviewScreenComponents.SeasonPage(
    innerPadding: PaddingValues,
    viewModel: SeasonsScreenViewModel,
    seasonBaseData: SeasonsScreenViewModelHelper.SeasonBaseDetails,
) {
    val seasonModel = viewModel.viewModelForSeason(seasonBaseData.tmdbSeasonId)
    SeasonDetailsContent(
        innerPadding = innerPadding,
        viewModel = viewModel,
        seasonModel = seasonModel,
    )
}

@Composable
private fun OverviewScreenComponents.SeasonDetailsContent(
    innerPadding: PaddingValues,
    viewModel: SeasonsScreenViewModel,
    seasonModel: SeasonsScreenViewModel.SeasonViewModel,
) {
    val navController = LocalNavController.current
    val episodes = seasonModel.details.mapResult { it.episodes }
    LoadableScreen(
        episodes,
        onError = { exception ->
            DefaultErrorScreen(
                errorMessage = exception.title.string(),
                errorDetails = exception.details?.string(),
                retry = { viewModel.retryAll() },
            )
        },
    ) { episodes ->
        ContentList(
            innerPadding.plus(PaddingValues(vertical = 16.dp, horizontal = 8.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsWithPosition(episodes) { position, (episodeId, episode) ->
                EpisodeListItem(
                    episode = episode,
                    onClick = { navController.navigateToEpisode(episodeId) },
                    position = position,
                )
            }
        }
    }
}
