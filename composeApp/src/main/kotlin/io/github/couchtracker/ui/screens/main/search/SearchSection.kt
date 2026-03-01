package io.github.couchtracker.ui.screens.main.search

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AppBarWithSearch
import androidx.compose.material3.ExpandedFullScreenSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarState
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import io.github.couchtracker.R
import io.github.couchtracker.ui.components.BackgroundTopAppBar
import io.github.couchtracker.utils.str
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

typealias SearchMediaFilters = Set<SearchableMediaType>

sealed class SearchScreenEvent {
    object FocusSearch : SearchScreenEvent()
}

val SEARCH_SCREEN_EVENT_BUS = MutableSharedFlow<SearchScreenEvent>(
    extraBufferCapacity = 1,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSection(innerPadding: PaddingValues) {
    val viewModel = viewModel {
        SearchViewModel(SearchableMediaType.entries.toSet())
    }
    val cs = rememberCoroutineScope()
    val searchBarState = rememberSearchBarState()
    val scrollBehavior = SearchBarDefaults.enterAlwaysSearchBarScrollBehavior(canScroll = { false })
    val bgColor = MaterialTheme.colorScheme.background

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        SEARCH_SCREEN_EVENT_BUS.collect { event ->
            when (event) {
                is SearchScreenEvent.FocusSearch -> {
                    focusRequester.requestFocus()
                }
            }
        }
    }

    val inputField = @Composable {
        SearchInputField(
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            viewModel = viewModel,
            searchBarState = searchBarState,
            coroutineScope = cs,
        )
    }
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BackgroundTopAppBar(
                contentOffset = { scrollBehavior.contentOffset },
                collapsedFraction = { 0f },
                image = { modifier, _ ->
                    AsyncImage(
                        modifier = modifier,
                        model = R.drawable.aurora_borealis,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                    )
                },
                backgroundColor = { bgColor },
                appBar = {
                    val color = lerp(
                        MaterialTheme.colorScheme.surfaceContainer,
                        MaterialTheme.colorScheme.background,
                        searchBarState.progress,
                    )
                    AppBarWithSearch(
                        // Plus default padding, 16.dp
                        modifier = Modifier.padding(horizontal = 8.dp),
                        scrollBehavior = scrollBehavior,
                        state = searchBarState,
                        inputField = inputField,
                        colors = SearchBarDefaults.appBarWithSearchColors(
                            searchBarColors = SearchBarDefaults.colors(
                                containerColor = color,
                            ),
                            scrolledAppBarContainerColor = Color.Transparent,
                            appBarContainerColor = Color.Transparent,
                            scrolledSearchBarContainerColor = color,
                        ),
                    )
                    ExpandedFullScreenSearchBar(
                        state = searchBarState,
                        inputField = inputField,
                        colors = SearchBarDefaults.colors(
                            containerColor = color,
                        ),
                    ) {
                        FullscreenSearchContent(viewModel)
                    }
                },
            )
        },
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets.only(WindowInsetsSides.Top),
    ) { padding ->
        SearchExplorePage(padding, viewModel)
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchInputField(
    modifier: Modifier,
    viewModel: SearchViewModel,
    searchBarState: SearchBarState,
    coroutineScope: CoroutineScope,
) {
    val searchParameters = viewModel.searchParameters
    SearchBarDefaults.InputField(
        modifier = modifier,
        query = searchParameters.query,
        onQueryChange = {
            viewModel.search(incomplete = true, query = it)
        },
        onSearch = {
            viewModel.search(incomplete = false, query = it)
        },
        expanded = searchBarState.currentValue == SearchBarValue.Expanded,
        onExpandedChange = { expand ->
            coroutineScope.launch {
                if (expand) {
                    searchBarState.animateToExpanded()
                } else {
                    searchBarState.animateToCollapsed()
                }
            }
        },
        placeholder = {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(1 - searchBarState.progress)
                    .clearAndSetSemantics {},
                text = R.string.search_placeholder.str(),
                textAlign = TextAlign.Center,
            )
        },
        leadingIcon = {
            Icon(Icons.Filled.Search, contentDescription = null)
        },
        trailingIcon = {
            IconButton(
                modifier = Modifier.scale(searchBarState.progress),
                onClick = {
                    viewModel.clearResults()
                    coroutineScope.launch { searchBarState.animateToCollapsed() }
                },
                content = { Icon(Icons.Default.Close, contentDescription = null) },
            )
        },
    )
}
