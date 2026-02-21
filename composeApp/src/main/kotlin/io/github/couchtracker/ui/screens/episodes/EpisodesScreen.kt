@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.episodes

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.plus
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarDefaults.floatingToolbarVerticalNestedScroll
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.UnknownExternalEpisodeId
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetScaffold
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val LOG_TAG = "EpisodeScreen"

@Serializable
data class EpisodeScreen(val episodeId: String) : Screen() {
    @Composable
    override fun content() {
        val externalEpisodeId = ExternalEpisodeId.parse(this@EpisodeScreen.episodeId)
        val seasonId = when (externalEpisodeId) {
            is TmdbExternalEpisodeId -> {
                externalEpisodeId.id.seasonId
            }
            is UnknownExternalEpisodeId -> TODO()
        }

        Content(
            externalEpisodeId,
            viewModel {
                EpisodesScreenViewModel(
                    application = checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]),
                    seasonId = seasonId,
                )
            },
        )
    }
}

fun NavController.navigateToEpisode(id: ExternalEpisodeId) {
    navigate(EpisodeScreen(id.serialize()))
}

@Composable
private fun Content(initialEpisode: ExternalEpisodeId, viewModel: EpisodesScreenViewModel) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadableScreen(
            data = viewModel.seasonDetails,
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = { viewModel.retryAll() },
                    )
                }
            },
        ) { seasonDetails ->
            EpisodeScreenContent(
                viewModel = viewModel,
                initialEpisode = initialEpisode,
                seasonDetails = seasonDetails,
                totalHeight = constraints.maxHeight,
                onRetry = { viewModel.retryAll() },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeScreenContent(
    viewModel: EpisodesScreenViewModel,
    initialEpisode: ExternalEpisodeId,
    seasonDetails: EpisodesScreenViewModelHelper.SeasonDetails,
    totalHeight: Int,
    onRetry: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val snackbarHostState = remember { SnackbarHostState() }
    OverviewScreenComponents.ShowSnackbarOnErrorEffect(
        snackbarHostState = snackbarHostState,
        loadable = { viewModel.allLoadables },
        onRetry = onRetry,
    )
    val pagerState = rememberPagerState(
        initialPage = seasonDetails.episodes.indexOfFirst { it.externalId == initialEpisode },
        pageCount = { seasonDetails.episodes.size },
    )
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    logCompositions(LOG_TAG, "Recomposing EpisodeScreenContent")
    MaterialTheme(colorScheme = colorScheme) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = backgroundColor,
            topBar = {
                OverviewScreenComponents.Header(
                    title = seasonDetails.name,
                    subtitle = viewModel.seasonSubtitle.resultValueOrNull(),
                    backdrop = viewModel.showBaseDetails.resultValueOrNull()?.backdrop,
                    scrollBehavior = scrollBehavior,
                    backgroundColor = { backgroundColor },
                    belowAppBar = {
                        OverviewScreenComponents.HeaderScrollableTabRow(
                            pagerState = pagerState,
                            tabText = { page ->
                                seasonDetails.episodes[page].number
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
                    val episodeDetails = seasonDetails.episodes[page]
                    OverviewScreenComponents.EpisodePage(
                        innerPadding = innerPadding,
                        viewModel = viewModel,
                        episodeDetails = episodeDetails,
                        totalHeight = totalHeight,
                    )
                }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun OverviewScreenComponents.EpisodePage(
    innerPadding: PaddingValues,
    viewModel: EpisodesScreenViewModel,
    episodeDetails: EpisodesScreenViewModelHelper.EpisodeBaseDetails,
    totalHeight: Int,
) {
    val episodeModel = viewModel.viewModelForEpisode(episodeDetails.tmdbEpisodeId)
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    WatchedItemSheetScaffold(
        scaffoldState = scaffoldState,
        watchedItemType = WatchedItemType.EPISODE,
        mediaRuntime = { episodeDetails.runtime },
        mediaLanguages = { emptyList() },
        containerColor = { Color.Transparent },
    ) {
        Scaffold(
            floatingActionButton = {
                EpisodeToolbar(
                    externalEpisodeId = episodeDetails.externalId,
                    expanded = toolbarExpanded,
                    onMarkAsWatched = {
                        // TODO: open watched scaffold
                    },
                )
            },
            contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Bottom),
        ) { padding ->
            EpisodeDetailsContent(
                innerPadding = innerPadding + padding,
                episodeModel = episodeModel,
                episodeDetails = episodeDetails,
                totalHeight = totalHeight,
                modifier = Modifier.floatingToolbarVerticalNestedScroll(
                    expanded = toolbarExpanded,
                    onExpand = { toolbarExpanded = true },
                    onCollapse = { toolbarExpanded = false },
                ),
            )
        }
    }
}

@Composable
private fun OverviewScreenComponents.EpisodeDetailsContent(
    innerPadding: PaddingValues,
    episodeModel: EpisodesScreenViewModel.EpisodeViewModel,
    episodeDetails: EpisodesScreenViewModelHelper.EpisodeBaseDetails,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    val overview = episodeModel.details.mapResult { it.overview }
    val guests = episodeModel.details.mapResult { it.guestStars }
    val crew = episodeModel.details.mapResult { it.crew }
    val tags = listOfNotNull(
        episodeDetails.tmdbRating?.formatted,
        episodeDetails.runtimeString,
    )
    ContentList(innerPadding, modifier) {
        topSpace()
        section({ textBlock("airDate", Loadable.value(episodeDetails.firstAirDate)) }) {
            tags(Loadable.value(tags))
        }
        section(title = { title(Loadable.value(episodeDetails.name)) }) {
            overview(overview)
        }
        imagesSection(episodeModel.images, totalHeight = totalHeight)
        castSection(
            people = guests,
            totalHeight = totalHeight,
            title = { textBlock("guest-title", R.string.section_guest_stars) },
        )
        crewSection(crew, totalHeight = totalHeight)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun EpisodeToolbar(
    externalEpisodeId: ExternalEpisodeId,
    expanded: Boolean,
    onMarkAsWatched: () -> Unit,
) {
    val fullProfileData = LocalFullProfileDataContext.current
    val watchCount = fullProfileData.watchedItems.count { it.itemId == externalEpisodeId }
    HorizontalFloatingToolbar(
        expanded = expanded,
        floatingActionButton = {
            FloatingToolbarDefaults.StandardFloatingActionButton(onClick = onMarkAsWatched) {
                Icon(Icons.Filled.Check, R.string.mark_episode_as_watched.str())
            }
        },
        content = {
            IconButton(onClick = { /* TODO */ }) {
                Icon(Icons.AutoMirrored.Default.List, contentDescription = "TODO") // TODO
            }
            BadgedBox(
                badge = {
                    if (watchCount > 0) {
                        Badge { Text(watchCount.toString()) }
                    }
                },
            ) {
                IconButton(onClick = { /* TODO: navigate to viewing history */ }) {
                    Icon(Icons.Default.Checklist, contentDescription = R.string.viewing_history.str())
                }
            }
        },
    )
}
