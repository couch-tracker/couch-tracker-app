@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import io.github.couchtracker.intl.formatAndList
import io.github.couchtracker.tmdb.TmdbLanguage
import io.github.couchtracker.tmdb.TmdbShow
import io.github.couchtracker.ui.Screen
import io.github.couchtracker.ui.components.DefaultErrorScreen
import io.github.couchtracker.ui.components.LoadableScreen
import io.github.couchtracker.ui.components.OverviewScreenComponents
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

@Serializable
data class ShowScreen(val showId: String, val language: String) : Screen() {
    @Composable
    override fun content() {
        val tmdbShow = when (val showId = ExternalShowId.parse(this@ShowScreen.showId)) {
            is TmdbExternalShowId -> {
                TmdbShow(showId.id, TmdbLanguage.parse(language))
            }

            is UnknownExternalShowId -> TODO()
        }
        Content(tmdbShow)
    }
}

fun NavController.navigateToShow(show: TmdbShow) {
    navigate(ShowScreen(show.id.toExternalId().serialize(), show.language.serialize()))
}

private enum class ShowScreenTab {
    DETAILS,
    SEASONS,
    VIEWING_HISTORY,
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
        suspend fun load() {
            screenModel = Loadable.Loading
            screenModel = loadShow(
                ctx,
                tmdbCache,
                show,
                width = this.constraints.maxWidth,
                height = this.constraints.maxHeight,
            )
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
            MaterialTheme(colorScheme = model.colorScheme) {
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    containerColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        OverviewScreenComponents.Header(
                            title = model.name,
                            backdrop = model.backdrop,
                            scrollBehavior = scrollBehavior,
                            titleReplacement = { isExpanded ->
                                OverviewScreenComponents.HeaderTitleTabRow(
                                    pagerState = pagerState,
                                    isExpanded = isExpanded,
                                    tabText = { page ->
                                        when (ShowScreenTab.entries[page]) {
                                            ShowScreenTab.DETAILS -> model.name
                                            ShowScreenTab.SEASONS -> R.string.tab_show_seasons.str()
                                            ShowScreenTab.VIEWING_HISTORY -> R.string.tab_show_viewing_history.str()
                                        }
                                    },
                                    onPageClick = { page ->
                                        coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                    },
                                )
                            },
                        )
                    },
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
            ShowScreenTab.VIEWING_HISTORY -> Text(R.string.tab_show_viewing_history.str(), Modifier.padding(innerPadding))
            ShowScreenTab.SEASONS -> Text(R.string.tab_show_seasons.str(), Modifier.padding(innerPadding))
            ShowScreenTab.DETAILS -> ShowDetailsContent(
                innerPadding = innerPadding,
                model = model,
                totalHeight = totalHeight,
            )
        }
    }
}

@Composable
private fun ShowDetailsContent(
    innerPadding: PaddingValues,
    model: ShowScreenModel,
    totalHeight: Int,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = innerPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverviewScreenComponents.run {
            space()

            val createdBy = model.createdBy
            val tags = listOfNotNull(
                model.year?.toString(),
                model.rating?.format(),
            ) + model.genres.map { it.name }
            if (createdBy.isNotEmpty() || tags.isNotEmpty()) {
                if (createdBy.isNotEmpty()) {
                    val creators = formatAndList(createdBy.map { it.name })
                    item {
                        Text(R.string.show_by_creator.str(creators))
                    }
                }
                tagsComposable(tags = tags)
                space()
            }

            textSection(model.tagline, model.overview)
            imagesSection(model.images, totalHeight = totalHeight)
            castSection(model.cast, totalHeight = totalHeight)
            crewSection(model.crew, totalHeight = totalHeight)
            bottomSpace()
        }
    }
}
