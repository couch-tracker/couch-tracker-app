package io.github.couchtracker.ui.screens.seasons

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.ExternalSeasonId
import io.github.couchtracker.db.profile.externalids.TmdbExternalSeasonId
import io.github.couchtracker.db.profile.externalids.UnknownExternalSeasonId
import io.github.couchtracker.ui.AnimationDefaults
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.CouchTrackerScreenScaffold
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.EpisodeListItem
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.itemsWithPosition
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.viewModelApplication
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

private const val LOG_TAG = "SeasonsScreen"

@Serializable
data class SeasonsScreen(val seasonId: String, val episodeId: String?) : Screen() {

    @Composable
    override fun Content() {
        val externalSeasonId = ExternalSeasonId.parse(this@SeasonsScreen.seasonId)
        val seasonId = when (externalSeasonId) {
            is TmdbExternalSeasonId -> externalSeasonId.id
            is UnknownExternalSeasonId -> TODO()
        }
        val externalEpisodeId = this@SeasonsScreen.episodeId?.let {
            ExternalEpisodeId.parse(it)
        }

        val viewModel = viewModel {
            SeasonsScreenViewModel(
                application = viewModelApplication(),
                showId = seasonId.showId,
                pendingPulseAnimation = externalEpisodeId,
            )
        }
        val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
        ScreenContainer(colorScheme) {
            Content(
                viewModel,
                initialSeason = externalSeasonId,
            )
        }
    }
}

fun NavController.navigateToSeason(id: ExternalSeasonId, episodeId: ExternalEpisodeId? = null) {
    navigate(
        SeasonsScreen(
            seasonId = ExternalSeasonId.serialize(id),
            episodeId = episodeId?.let { ExternalEpisodeId.serialize(it) },
        ),
    )
}

@Composable
private fun Content(
    viewModel: SeasonsScreenViewModel,
    initialSeason: ExternalSeasonId,
) {
    LoadableScreen(
        data = viewModel.showDetails,
        onError = { apiError ->
            DefaultErrorScreen(
                error = apiError,
                retry = { viewModel.retryAll() },
            )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SeasonsScreenContent(
    viewModel: SeasonsScreenViewModel,
    initialSeason: ExternalSeasonId,
    showDetails: SeasonsScreenViewModelHelper.ShowDetails,
    reloadSeason: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        error = { viewModel.aggregateError },
        onRetry = reloadSeason,
    )
    val pagerState = rememberPagerState(
        initialPage = showDetails.seasons.indexOfFirst { it.externalId == initialSeason },
        pageCount = { showDetails.seasons.size },
    )
    val selectedSeason = showDetails.seasons[pagerState.currentPage]
    logCompositions(LOG_TAG, "Recomposing SeasonsScreenContent")
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    CouchTrackerScreenScaffold(
        title = { selectedSeason.name ?: selectedSeason.defaultName },
        subtitle = { showDetails.name },
        backdrop = { showDetails.backdrop },
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
        snackbarHostState = snackbarHostState,
        scrollBehavior = scrollBehavior,
        content = { innerPadding ->
            HorizontalPager(pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                val seasonDetails = showDetails.seasons[page]
                OverviewScreenComponents.SeasonPage(
                    innerPadding = innerPadding,
                    viewModel = viewModel,
                    showDetails = showDetails,
                    seasonBaseData = seasonDetails,
                    nestedScrollBehavior = scrollBehavior,
                )
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewScreenComponents.SeasonPage(
    innerPadding: PaddingValues,
    viewModel: SeasonsScreenViewModel,
    seasonBaseData: SeasonsScreenViewModelHelper.SeasonBaseDetails,
    showDetails: SeasonsScreenViewModelHelper.ShowDetails,
    nestedScrollBehavior: TopAppBarScrollBehavior,
) {
    val originalLanguage by rememberUpdatedState(showDetails.originalLanguage)
    val seasonModel = viewModel.viewModelForSeason(seasonBaseData.tmdbSeasonId, showOriginalLanguage = { originalLanguage })
    SeasonDetailsContent(
        innerPadding = innerPadding,
        viewModel = viewModel,
        seasonModel = seasonModel,
        nestedScrollBehavior = nestedScrollBehavior,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OverviewScreenComponents.SeasonDetailsContent(
    innerPadding: PaddingValues,
    viewModel: SeasonsScreenViewModel,
    seasonModel: SeasonsScreenViewModel.SeasonViewModel,
    nestedScrollBehavior: TopAppBarScrollBehavior,
) {
    val episodes = seasonModel.details.mapResult { it.episodes }
    LoadableScreen(
        episodes,
        onError = { apiError ->
            DefaultErrorScreen(
                error = apiError,
                retry = { viewModel.retryAll() },
            )
        },
    ) { episodes ->
        val indexToPulse = episodes.indexOfFirst { it.episodeId == viewModel.pendingPulseAnimation }
        val listState = rememberLazyListState(
            initialFirstVisibleItemIndex = indexToPulse.coerceAtLeast(0),
            // Scrolling dow to give some "breathing room"
            initialFirstVisibleItemScrollOffset = -with(LocalDensity.current) { 200.dp.toPx() }.roundToInt(),
        )
        LaunchedEffect(Unit) {
            // Since I might have scrolled, I need to collapse the header if the first item has been scrolled up
            val firstItemMeasurementInfo = listState.layoutInfo.visibleItemsInfo.find { it.index == 0 }
            if (viewModel.pendingPulseAnimation != null && (firstItemMeasurementInfo == null || firstItemMeasurementInfo.offset != 0)) {
                // Collapse the header
                nestedScrollBehavior.state.apply {
                    heightOffset = heightOffsetLimit
                    contentOffset = -heightOffsetLimit
                }
            }
        }
        ContentList(
            innerPadding.plus(PaddingValues(vertical = 16.dp, horizontal = 8.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            state = listState,
        ) {
            itemsWithPosition(episodes) { position, episode ->
                val defaultListItemColors = ListItemDefaults.colors()
                val colors = if (episode.episodeId == viewModel.pendingPulseAnimation) {
                    val pulseInterpolation = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        AnimationDefaults.PulseAnimation.run(pulseInterpolation)
                        viewModel.pendingPulseAnimation = null
                    }
                    val pulseColor = lerp(
                        start = defaultListItemColors.containerColor,
                        stop = MaterialTheme.colorScheme.onSurface,
                        fraction = pulseInterpolation.value,
                    )
                    defaultListItemColors.copy(
                        containerColor = pulseColor,
                        // Note: parameter necessary to use non-deprecated copy function
                        selectedContainerColor = Color.Unspecified,
                    )
                } else {
                    defaultListItemColors
                }
                EpisodeListItem(
                    episode = episode,
                    position = position,
                    colors = colors,
                )
            }
        }
    }
}
