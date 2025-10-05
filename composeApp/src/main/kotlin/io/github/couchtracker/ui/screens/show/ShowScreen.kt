@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.expandVertically
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.navigation.NavController
import io.github.couchtracker.R
import io.github.couchtracker.db.profile.show.ExternalShowId
import io.github.couchtracker.db.profile.show.TmdbExternalShowId
import io.github.couchtracker.db.profile.show.UnknownExternalShowId
import io.github.couchtracker.settings.appSettings
import io.github.couchtracker.tmdb.BaseTmdbShow
import io.github.couchtracker.tmdb.TmdbBaseMemoryCache
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.tmdb.TmdbShowId
import io.github.couchtracker.ui.ColorSchemes
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.ui.components.WipMessageComposable
import io.github.couchtracker.utils.ApiLoadable
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.awaitAsLoadable
import io.github.couchtracker.utils.logExecutionTime
import io.github.couchtracker.utils.mapResult
import io.github.couchtracker.utils.resultValueOrNull
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.valueOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.mp.KoinPlatform

private const val LOG_TAG = "ShowScreen"

@Serializable
data class ShowScreen(val showId: String) : Screen() {
    @Composable
    override fun content() {
        when (val showId = ExternalShowId.parse(this@ShowScreen.showId)) {
            is TmdbExternalShowId -> {
                val tmdbLanguages = appSettings().get { Tmdb.Languages }.current
                Content(TmdbShow(showId.id, tmdbLanguages))
            }
            is UnknownExternalShowId -> TODO()
        }
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
private fun Content(show: TmdbShow) {
    val coroutineScope = rememberCoroutineScope()
    val ctx = LocalContext.current
    var screenModel by remember { mutableStateOf<ApiLoadable<ShowScreenModel>>(Loadable.Loading) }
    val pagerState = rememberPagerState(
        initialPage = ShowScreenTab.entries.indexOf(ShowScreenTab.DETAILS),
        pageCount = { ShowScreenTab.entries.size },
    )
    suspend fun load() {
        screenModel = Loadable.Loading
        screenModel = coroutineScope.async(Dispatchers.Default) {
            logExecutionTime(LOG_TAG, "Loading show") {
                Loadable.Loaded(
                    coroutineScope.loadShow(ctx = ctx, show = show),
                )
            }
        }.await()
    }
    LaunchedEffect(show) {
        load()
    }

    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
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
            val colorScheme = model.colorScheme.awaitAsLoadable().valueOrNull() ?: ColorSchemes.Show
            val backgroundColor by animateColorAsState(colorScheme.background)
            MaterialTheme(colorScheme = colorScheme) {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        OverviewScreenComponents.Header(
                            title = model.name,
                            backgroundColor = { backgroundColor },
                            backdrop = model.backdrop,
                            scrollBehavior = scrollBehavior,
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
    val fullDetails = model.fullDetails.awaitAsLoadable()
    val images = model.images.awaitAsLoadable()
    val credits = model.credits.awaitAsLoadable()
    ContentList(innerPadding) {
        topSpace()
        section("subtitle") {
            val creators = fullDetails.mapResult { it.createdByString }.resultValueOrNull()
            val tags = fullDetails.mapResult { details ->
                listOfNotNull(
                    model.year?.toString(),
                    model.rating?.formatted,
                ) + details.genres.map { it.name }
            }.resultValueOrNull().orEmpty()

            val hasCreatedBy = creators != null
            val hasTags = tags.isNotEmpty()
            if (hasCreatedBy || hasTags) {
                item(key = "subtitle-content") {
                    AnimatedVisibility(hasCreatedBy, enter = expandVertically()) {
                        Paragraph(creators)
                    }
                    SpaceBetweenItems()
                    AnimatedVisibility(hasTags, enter = expandVertically()) {
                        TagsComposable(tags)
                    }
                }
            }
        }
        paragraphSection("overview", fullDetails.mapResult { it.tagline }.resultValueOrNull(), model.overview)
        imagesSection(images, totalHeight = totalHeight)
        castSection(credits.mapResult { it.cast }, totalHeight = totalHeight)
        crewSection(credits.mapResult { it.crew }, totalHeight = totalHeight)
    }
}
