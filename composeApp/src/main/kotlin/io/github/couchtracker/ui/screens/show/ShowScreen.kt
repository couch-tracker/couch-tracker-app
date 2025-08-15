@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.db.profile.show.TmdbExternalShowId
import io.github.couchtracker.db.profile.show.UnknownExternalShowId
import io.github.couchtracker.db.tmdbCache.TmdbCache
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbMemoryCache
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableContainer
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.awaitLoadable
import io.github.couchtracker.utils.awaitResult
import io.github.couchtracker.utils.isLoadingOr
import io.github.couchtracker.utils.logExecutionTime
import io.github.couchtracker.utils.map
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private const val LOG_TAG = "ShowScreen"

@Serializable
data class ShowScreen(val showId: String, val language: String) : Screen() {
    @Composable
    override fun content() {
        val tmdbShow = when (val showId = ExternalShowId.parse(showId)) {
            is TmdbExternalShowId -> {
                TmdbShow(showId.id, TmdbLanguage.parse(language))
            }

            is UnknownExternalShowId -> TODO()
        }
        Content(tmdbShow)
    }
}

fun NavController.navigateToShow(show: TmdbShow, preloadData: BaseTmdbShow?) {
    if (preloadData != null) {
        TmdbMemoryCache.registerItem(preloadData)
    }
    navigate(ShowScreen(show.id.toExternalId().serialize(), show.language.serialize()))
}

private enum class ShowScreenTab {
    VIEWING_HISTORY,
    SEASONS,
    DETAILS,
}

@Composable
private fun Content(show: TmdbShow) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val tmdbCache = koinInject<TmdbCache>()
    var screenModel by remember { mutableStateOf<Loadable<ShowScreenModel, ApiException>>(Loadable.Loading) }
    val pagerState = rememberPagerState(
        initialPage = ShowScreenTab.entries.indexOf(ShowScreenTab.DETAILS),
        pageCount = { ShowScreenTab.entries.size },
    )

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        val maxWidth = this.constraints.maxWidth
        val maxHeight = this.constraints.maxHeight
        suspend fun load() {
            if (screenModel is Result.Error) {
                screenModel = Loadable.Loading
            }
            screenModel = coroutineScope.async(Dispatchers.Default) {
                logExecutionTime(LOG_TAG, "Loading show") {
                    coroutineScope.loadShow(
                        ctx,
                        tmdbCache,
                        show,
                        width = maxWidth,
                        height = maxHeight,
                    )
                }
            }.await()
        }
        LaunchedEffect(show) {
            load()
        }

        LoadableScreen(
            data = screenModel,
            onError = { exception ->
                Surface {
                    DefaultErrorScreen(
                        errorMessage = exception.title.string(),
                        errorDetails = exception.details?.string(),
                        retry = {
                            coroutineScope.launch { load() }
                        },
                    )
                }
            },
        ) { model ->
            val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
            val snackbarHostState = remember { SnackbarHostState() }
            OverviewScreenComponents.ShowSnackbarOnErrorEffect(
                snackbarHostState = snackbarHostState,
                state = model.allDeferred,
                onRetry = {
                    coroutineScope.launch { load() }
                },
            )
            // TODO
            val cs = model.colorScheme.awaitLoadable().valueOrNull() ?: ColorSchemes.Show
            val backgroundColor by animateColorAsState(cs.background)
            MaterialTheme(colorScheme = cs) {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = backgroundColor,
                    topBar = {
                        OverviewScreenComponents.Header(
                            title = model.name,
                            backdrop = model.backdrop,
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
                        ShowScreenContent(
                            innerPadding = innerPadding,
                            totalHeight = constraints.maxHeight,
                            model = model,
                            pagerState = pagerState,
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun ShowScreenContent(
    innerPadding: PaddingValues,
    model: ShowScreenModel,
    totalHeight: Int,
    pagerState: PagerState,
) {
    HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (ShowScreenTab.entries[page]) {
            ShowScreenTab.DETAILS -> OverviewScreenComponents.ShowDetailsContent(
                innerPadding = innerPadding,
                model = model,
                totalHeight = totalHeight,
            )
            ShowScreenTab.SEASONS -> WipMessageComposable(gitHubIssueId = 138)
            ShowScreenTab.VIEWING_HISTORY -> WipMessageComposable(gitHubIssueId = 131)
        }
    }
}

@Composable
private fun OverviewScreenComponents.ShowDetailsContent(
    innerPadding: PaddingValues,
    model: ShowScreenModel,
    totalHeight: Int,
) {
    val fullDetails = model.fullDetails.awaitResult()
    val images = model.images.awaitResult()
    val credits = model.credits.awaitResult()
    ContentList(innerPadding) {
        topSpace()
        section("subtitle", fullDetails.map { it.createdByString }, titlePlaceholderLines = 2) {
            val tags = fullDetails.map { details ->
                listOfNotNull(
                    model.year?.toString(),
                    model.rating?.formatted,
                ) + details.genres.map { it.name }
            }
            if (tags.isLoadingOr { it.isNotEmpty() }) {
                item("subtitle-content") {
                    LoadableContainer(
                        tags,
                        content = { TagsComposable(tags = it) },
                        placeholder = { Spacer(Modifier.height(40.dp)) },
                    )
                }
            }
        }
        paragraphSection("overview", fullDetails.map { it.tagline }, Result.Value(model.overview), titlePlaceholderLines = 2)
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.map { it.cast }, totalHeight = totalHeight)
        crewSection(credits.map { it.crew }, totalHeight = totalHeight)
    }
}
