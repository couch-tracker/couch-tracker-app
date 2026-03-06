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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.couchtracker.LocalFullProfileDataContext
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.ProfileData
import io.github.couchtracker.db.profile.WatchedEpisodeSession
import io.github.couchtracker.db.profile.externalids.ExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.TmdbExternalEpisodeId
import io.github.couchtracker.db.profile.externalids.UnknownExternalEpisodeId
import io.github.couchtracker.db.profile.model.watchedItem.ModalWatchedEpisodeSessionSelectorBottomSheet
import io.github.couchtracker.db.profile.model.watchedItem.WatchedItemType
import io.github.couchtracker.db.profile.model.watchedItem.rememberModalWatchedEpisodeSessionSelectorBottomSheetState
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.WatchableMediaScreenScaffold
import io.github.couchtracker.ui.components.WatchedItemsIconButton
import io.github.couchtracker.ui.screens.watchedItem.WatchedItemSheetMode
import io.github.couchtracker.ui.screens.watchedItem.rememberWatchedItemSheetScaffoldState
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.logCompositions
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.viewModelApplication
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
                    application = viewModelApplication(),
                    seasonId = seasonId,
                )
            },
        )
    }
}

fun NavController.navigateToEpisode(id: ExternalEpisodeId) {
    navigate(EpisodeScreen(ExternalEpisodeId.serialize(id)))
}

@Composable
private fun Content(initialEpisode: ExternalEpisodeId, viewModel: EpisodesScreenViewModel) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        LoadableScreen(
            data = viewModel.seasonDetails,
            onError = { apiError ->
                Surface {
                    DefaultErrorScreen(
                        apiError = apiError,
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
    val fullProfileData = LocalFullProfileDataContext.current
    val coroutineScope = rememberCoroutineScope()
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
    val selectedEpisode = seasonDetails.episodes[pagerState.currentPage]
    val colorScheme = viewModel.colorScheme.resultValueOrNull() ?: ColorSchemes.Show
    val backgroundColor by animateColorAsState(colorScheme.background)
    val scaffoldState = rememberWatchedItemSheetScaffoldState()
    val watchSessionSelectorState = rememberModalWatchedEpisodeSessionSelectorBottomSheetState()
    logCompositions(LOG_TAG, "Recomposing EpisodeScreenContent")
    MaterialTheme(colorScheme = colorScheme) {
        WatchableMediaScreenScaffold(
            colorScheme = colorScheme,
            backgroundColor = { backgroundColor },
            watchedItemSheetScaffoldState = scaffoldState,
            watchedItemType = WatchedItemType.EPISODE,
            mediaRuntime = { selectedEpisode.runtime },
            mediaLanguages = { emptyList() }, // TODO
            title = selectedEpisode.name ?: selectedEpisode.number,
            subtitle = viewModel.seasonSubtitle.resultValueOrNull(),
            backdrop = viewModel.showBaseDetails.resultValueOrNull()?.backdrop,
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
            snackbarHostState = snackbarHostState,
            content = { innerPadding ->
                if (watchSessionSelectorState.showBottomSheet) {
                    ModalWatchedEpisodeSessionSelectorBottomSheet(watchSessionSelectorState)
                }
                HorizontalPager(pagerState, modifier = Modifier.fillMaxSize(), beyondViewportPageCount = 1) { page ->
                    val episodeDetails = seasonDetails.episodes[page]
                    OverviewScreenComponents.EpisodePage(
                        innerPadding = innerPadding,
                        viewModel = viewModel,
                        episodeDetails = episodeDetails,
                        totalHeight = totalHeight,
                        onMarkAsWatched = {
                            val showId = viewModel.seasonId.showId.toExternalId()
                            val sessions = fullProfileData.watchedEpisodeSessions[showId].orEmpty()
                            if (sessions.size > 1) {
                                watchSessionSelectorState.open(
                                    sessions = sessions,
                                    onSelected = { watchSession ->
                                        scaffoldState.open(
                                            WatchedItemSheetMode.New.Episode(
                                                itemId = episodeDetails.externalId,
                                                sessionProvider = { watchSession.watchedEpisodeSession },
                                            ),
                                        )
                                    },
                                )
                            } else {
                                val sessionProvider: (ProfileData) -> WatchedEpisodeSession = if (sessions.isEmpty()) {
                                    { db ->
                                        db.watchedEpisodeSessionQueries.insert(
                                            showId = showId,
                                            name = null,
                                            description = null,
                                            isActive = true,
                                            defaultDimensionSelections = db.watchedItemDimensionSelectionsQueries.insert().executeAsOne(),
                                        ).executeAsOne()
                                    }
                                } else {
                                    { sessions.single().watchedEpisodeSession }
                                }
                                scaffoldState.open(
                                    WatchedItemSheetMode.New.Episode(
                                        itemId = episodeDetails.externalId,
                                        sessionProvider = sessionProvider,
                                    ),
                                )
                            }
                        },
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
    onMarkAsWatched: () -> Unit,
) {
    val episodeModel = viewModel.viewModelForEpisode(episodeDetails.tmdbEpisodeId)
    var toolbarExpanded by rememberSaveable { mutableStateOf(true) }
    Scaffold(
        floatingActionButton = {
            EpisodeToolbar(
                externalEpisodeId = episodeDetails.externalId,
                expanded = toolbarExpanded,
                onMarkAsWatched = onMarkAsWatched,
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
        section(title = {}) {
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
            WatchedItemsIconButton(externalEpisodeId)
        },
    )
}
