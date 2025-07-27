@file:OptIn(ExperimentalMaterial3Api::class)

package io.github.couchtracker.ui.screens.show

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
            var tabsInExpandedAppBar by remember { mutableStateOf(true) }
            var tabsInCollapsedAppBar by remember { mutableStateOf(true) }
            var tabsBelowAppBar by remember { mutableStateOf(false) }

            var titleInExpandedAppBar by remember { mutableStateOf(false) }
            var titleInCollapsedAppBar by remember { mutableStateOf(false) }
            var titleInContent by remember { mutableStateOf(true) }
            var titleInExpandedTab by remember { mutableStateOf(true) }
            var titleInCollapsedTab by remember { mutableStateOf(true) }

            var bigBoiFirst by remember { mutableStateOf(false) }
            var showByInExpandedTitle by remember { mutableStateOf(false) }
            fun preset2(){
                tabsInExpandedAppBar = false
                tabsInCollapsedAppBar = false
                tabsBelowAppBar = true

                bigBoiFirst = false

                titleInExpandedAppBar = true
                titleInCollapsedAppBar = true
                titleInExpandedTab = false
                titleInCollapsedTab = false
                titleInContent = false
            }
            remember { preset2() }

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
                                Column {
                                    if ((isExpanded && titleInExpandedAppBar) || (!isExpanded && titleInCollapsedAppBar)) {
                                        OverviewScreenComponents.HeaderTitle(model.name, isExpanded)
                                    }
                                    if (isExpanded && showByInExpandedTitle && model.createdBy.isNotEmpty()) {
                                        val creators = formatAndList(model.createdBy.map { it.name })
                                        Text(
                                            R.string.show_by_creator.str(creators),
                                            style = MaterialTheme.typography.titleMedium,
                                        )
                                    }
                                    if ((isExpanded && tabsInExpandedAppBar) || (!isExpanded && tabsInCollapsedAppBar)) {
                                        OverviewScreenComponents.HeaderTitleTabRow(
                                            pagerState = pagerState,
                                            bigBoiFirst = bigBoiFirst && isExpanded,
                                            tabText = { page ->
                                                when (ShowScreenTab.entries[page]) {
                                                    // TODO
                                                    ShowScreenTab.DETAILS -> if ((isExpanded && titleInExpandedTab) || (!isExpanded && titleInCollapsedTab)) {
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
                                }
                            },
                            belowAppBar = {
                                if (tabsBelowAppBar) {
                                    OverviewScreenComponents.HeaderTitleTabRow(
                                        pagerState = pagerState,
                                        bigBoiFirst = bigBoiFirst,
                                        tabText = { page ->
                                            when (ShowScreenTab.entries[page]) {
                                                // TODO
                                                ShowScreenTab.DETAILS -> if (titleInExpandedTab) {
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
                            showNameInContent = titleInContent,
                            showByInContent = !showByInExpandedTitle,
                        ) {
                            CompositionLocalProvider(LocalPreferenceTheme provides preferenceTheme()) {
                                Column(
                                    Modifier
                                        .padding(innerPadding)
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState()),
                                ) {
                                    CheckboxPreference(tabsInExpandedAppBar, { tabsInExpandedAppBar = it }, { Text("Tabs in expanded App Bar") })
                                    CheckboxPreference(tabsInCollapsedAppBar, { tabsInCollapsedAppBar = it }, { Text("Tabs in collapsed App Bar") })
                                    CheckboxPreference(tabsBelowAppBar, { tabsBelowAppBar = it }, { Text("Tabs below App Bar") })

                                    CheckboxPreference(titleInExpandedAppBar, { titleInExpandedAppBar = it }, { Text("Title in expanded App Bar") })
                                    CheckboxPreference(titleInCollapsedAppBar, { titleInCollapsedAppBar = it }, { Text("Title in collapsed App Bar") })
                                    CheckboxPreference(titleInContent, { titleInContent = it }, { Text("Title in content") })
                                    CheckboxPreference(titleInExpandedTab, { titleInExpandedTab = it }, { Text("Title in expanded tab") })
                                    CheckboxPreference(titleInCollapsedTab, { titleInCollapsedTab = it }, { Text("Title in collapsed tab") })

                                    CheckboxPreference(bigBoiFirst, { bigBoiFirst = it }, { Text("Big boi tab") })
                                    CheckboxPreference(
                                        showByInExpandedTitle,
                                        { showByInExpandedTitle = it },
                                        { Text("Show by in expanded title") },
                                    )

                                    Button(
                                        onClick = {
                                            tabsInExpandedAppBar = true
                                            tabsInCollapsedAppBar = true
                                            tabsBelowAppBar = false

                                            titleInExpandedAppBar = false
                                            titleInCollapsedAppBar = false
                                            titleInExpandedTab = false
                                            titleInCollapsedTab = true
                                            titleInContent = true

                                            bigBoiFirst = false

                                            showByInExpandedTitle = false
                                        },
                                    ) {
                                        Text("Preset 1")
                                    }
                                    Button(
                                        onClick = {
                                            preset2()
                                        },
                                    ) {
                                        Text("Preset 2")
                                    }
                                    Button(
                                        onClick = {
                                            tabsInExpandedAppBar = true
                                            tabsInCollapsedAppBar = true
                                            tabsBelowAppBar = false

                                            titleInExpandedAppBar = false
                                            titleInCollapsedAppBar = false
                                            titleInExpandedTab = true
                                            titleInCollapsedTab = true
                                            titleInContent = false

                                            bigBoiFirst = true

                                            showByInExpandedTitle = false
                                        },
                                    ) {
                                        Text("Preset 3")
                                    }
                                    Button(
                                        onClick = {
                                            tabsInExpandedAppBar = true
                                            tabsInCollapsedAppBar = true
                                            tabsBelowAppBar = false

                                            titleInExpandedAppBar = true
                                            titleInCollapsedAppBar = false
                                            titleInExpandedTab = false
                                            titleInCollapsedTab = true
                                            titleInContent = false

                                            bigBoiFirst = false

                                            showByInExpandedTitle = false
                                        },
                                    ) {
                                        Text("Preset 4")
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
    showByInContent: Boolean,
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
                showByInContent = showByInContent,
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
    showByInContent: Boolean,
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
            if (showByInContent && model.createdBy.isNotEmpty()) {
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
