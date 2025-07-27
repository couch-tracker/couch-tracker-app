@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import me.zhanghai.compose.preference.CheckboxPreference
import me.zhanghai.compose.preference.LocalPreferenceTheme
import me.zhanghai.compose.preference.preferenceTheme
import org.koin.compose.koinInject
import kotlin.random.Random

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
            var tabBelowAppBar by remember { mutableStateOf(false) }
            var bigBoiFirst by remember { mutableStateOf(false) }
            var showNameInTabs by remember { mutableStateOf(true) }
            var showNameInExpandedTab by remember { mutableStateOf(false) }
            var showNameInContent by remember { mutableStateOf(true) }
            var showNameInExpandedTitle by remember { mutableStateOf(false) }

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
                                if (!tabBelowAppBar) {
                                    OverviewScreenComponents.HeaderTitleTabRow(
                                        pagerState = pagerState,
                                        bigBoiFirst = bigBoiFirst && isExpanded,
                                        tabText = { page ->
                                            when (ShowScreenTab.entries[page]) {
                                                // TODO
                                                ShowScreenTab.DETAILS -> if ((isExpanded && showNameInExpandedTab) || (!isExpanded && showNameInTabs)) {
                                                    model.name
                                                } else {
                                                    "Details"
                                                }

                                                ShowScreenTab.SEASONS -> R.string.tab_show_seasons.str()
                                                ShowScreenTab.VIEWING_HISTORY -> R.string.tab_show_viewing_history.str()
                                            }
                                        },
                                        onPageClick = { page ->
                                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                        },
                                    )
                                } else if (showNameInExpandedTitle || !isExpanded) {
                                    OverviewScreenComponents.HeaderTitle(model.name, isExpanded)
                                }
                            },
                            belowAppBar = {
                                if (tabBelowAppBar) {
                                    OverviewScreenComponents.HeaderTitleTabRow(
                                        pagerState = pagerState,
                                        bigBoiFirst = bigBoiFirst,
                                        tabText = { page ->
                                            when (ShowScreenTab.entries[page]) {
                                                // TODO
                                                ShowScreenTab.DETAILS -> if (showNameInTabs) {
                                                    model.name
                                                } else {
                                                    "Details"
                                                }

                                                ShowScreenTab.SEASONS -> R.string.tab_show_seasons.str()
                                                ShowScreenTab.VIEWING_HISTORY -> R.string.tab_show_viewing_history.str()
                                            }
                                        },
                                        onPageClick = { page ->
                                            coroutineScope.launch { pagerState.animateScrollToPage(page) }
                                        },
                                    )
                                }
                            },
                        )
                    },
                    content = { innerPadding ->
                        ShowScreenContent(
                            innerPadding = innerPadding,
                            totalHeight = constraints.maxHeight,
                            model = model,
                            pagerState = pagerState,
                            showNameInContent = showNameInContent,
                        ) {
                            CompositionLocalProvider(LocalPreferenceTheme provides preferenceTheme()) {
                                Column(Modifier.padding(innerPadding).fillMaxSize().verticalScroll(rememberScrollState())) {
                                    CheckboxPreference(tabBelowAppBar, { tabBelowAppBar = it }, { Text("Tabs below App Bar") })
                                    CheckboxPreference(bigBoiFirst, { bigBoiFirst = it }, { Text("Big boi tab") })
                                    CheckboxPreference(showNameInTabs, { showNameInTabs = it }, { Text("Show name in tab") })
                                    CheckboxPreference(
                                        showNameInExpandedTab,
                                        { showNameInExpandedTab = it },
                                        { Text("Show name in expanded tab") },
                                    )
                                    CheckboxPreference(showNameInContent, { showNameInContent = it }, { Text("Show name in content") })
                                    CheckboxPreference(
                                        showNameInExpandedTitle,
                                        { showNameInExpandedTitle = it },
                                        { Text("Show name in expanded title") },
                                    )

                                    Button(
                                        onClick = {
                                            tabBelowAppBar = false
                                            bigBoiFirst = false
                                            showNameInTabs = true
                                            showNameInExpandedTab = false
                                            showNameInContent = true
                                            showNameInExpandedTitle = false
                                        },
                                    ) {
                                        Text("Preset 1")
                                    }
                                    Button(
                                        onClick = {
                                            tabBelowAppBar = true
                                            bigBoiFirst = false
                                            showNameInTabs = false
                                            showNameInExpandedTab = false
                                            showNameInContent = false
                                            showNameInExpandedTitle = true
                                        },
                                    ) {
                                        Text("Preset 2")
                                    }
                                    Button(
                                        onClick = {
                                            tabBelowAppBar = false
                                            bigBoiFirst = true
                                            showNameInTabs = true
                                            showNameInExpandedTab = true
                                            showNameInContent = false
                                            showNameInExpandedTitle = false
                                        },
                                    ) {
                                        Text("Preset 3")
                                    }
                                }
                            }
                        }
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
    showNameInContent: Boolean,
    controls: @Composable () -> Unit,
) {
    HorizontalPager(pagerState, modifier = Modifier.fillMaxSize()) { page ->
        when (ShowScreenTab.entries[page]) {
            ShowScreenTab.VIEWING_HISTORY -> Text(R.string.tab_show_viewing_history.str(), Modifier.padding(innerPadding))
            ShowScreenTab.SEASONS -> controls()
            ShowScreenTab.DETAILS -> ShowDetailsContent(
                innerPadding = innerPadding,
                model = model,
                totalHeight = totalHeight,
                showNameInContent = showNameInContent,
            )
        }
    }
}

@Composable
private fun ShowDetailsContent(
    innerPadding: PaddingValues,
    model: ShowScreenModel,
    totalHeight: Int,
    showNameInContent: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        contentPadding = innerPadding,
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OverviewScreenComponents.run {
            space()
            if (showNameInContent) {
                item {
                    Column {
                        Text(model.name, style = MaterialTheme.typography.headlineMedium)
                    }
                }
            }
            if (model.createdBy.isNotEmpty()) {
                val creators = formatAndList(model.createdBy.map { it.name })
                item {
                    Text(R.string.show_by_creator.str(creators))
                }
            }

            tagsComposable(
                tags = listOfNotNull(
                    model.year?.toString(),
                    model.rating?.format(),
                ) + model.genres.map { it.name },
            )
            space()

            textSection(model.tagline, model.overview)
            imagesSection(model.images, totalHeight = totalHeight)
            castSection(model.cast, totalHeight = totalHeight)
            crewSection(model.crew, totalHeight = totalHeight)
            bottomSpace()
        }
    }
}
