package io.github.couchtracker.ui.screens.main.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import coil3.compose.AsyncImage
import io.github.couchtracker.LocalNavController
import io.github.couchtracker.R
import io.github.couchtracker.ui.ItemPosition
import io.github.couchtracker.ui.ListItemShapes
import io.github.couchtracker.ui.components.MessageComposable
import io.github.couchtracker.ui.components.PaginatedGrid
import io.github.couchtracker.ui.components.PortraitComposableDefaults
import io.github.couchtracker.ui.components.TagsRow
import io.github.couchtracker.ui.rememberPlaceholderPainter
import io.github.couchtracker.utils.heightWithAspectRatio
import io.github.couchtracker.utils.str
import kotlinx.coroutines.flow.Flow

@Composable
fun FullscreenSearchContent(viewModel: SearchViewModel) {
    val searchParameters = viewModel.searchParameters
    LazyRow(
        modifier = Modifier
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Companion.CenterHorizontally),
    ) {
        items(SearchableMediaType.entries) { type ->
            val selected = type in searchParameters.filters
            MaterialTheme(colorScheme = type.colorScheme) {
                FilterChip(
                    selected = selected,
                    leadingIcon = {
                        Icon(
                            imageVector = if (selected) Icons.Filled.Done else Icons.Filled.Remove,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize),
                        )
                    },
                    label = { Text(type.title.str()) },
                    onClick = {
                        val newFilters = if (selected) {
                            if (searchParameters.filters.size > 1) {
                                searchParameters.filters - type
                            } else {
                                SearchableMediaType.entries.toSet()
                            }
                        } else {
                            searchParameters.filters + type
                        }
                        viewModel.search(incomplete = false, filters = newFilters)
                    },
                )
            }
        }
    }

    val currentSearchInstance by viewModel.currentSearchInstance.collectAsStateWithLifecycle(null)
    if (currentSearchInstance?.searchParameters?.isEmpty() == false) {
        SearchResults(results = viewModel.searchResults, lazyGridState = viewModel.lazyGridState)
    }
}

@Composable
private fun SearchResults(results: Flow<PagingData<SearchResultItem>>, lazyGridState: LazyGridState) {
    val contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 96.dp)
    val paginatedItems = results.collectAsLazyPagingItems()
    PaginatedGrid(
        paginatedItems = paginatedItems,
        columns = GridCells.Fixed(1),
        contentPadding = contentPadding,
        lazyGridState = lazyGridState,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        itemComposable = { item, index ->
            SearchResult(
                item,
                position = ItemPosition(index, paginatedItems.itemSnapshotList.size),
            )
        },
        emptyComposable = {
            MessageComposable(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                icon = Icons.Default.SearchOff,
                message = R.string.search_no_results.str(),
            )
        },
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchResult(
    item: SearchResultItem?,
    position: ItemPosition,
) {
    val navController = LocalNavController.current

    ListItem(
        onClick = { item?.navigate(navController) },
        leadingContent = {
            Surface(
                shape = MaterialTheme.shapes.small,
                modifier = Modifier.heightWithAspectRatio(
                    height = 96.dp,
                    aspectRatio = PortraitComposableDefaults.POSTER_ASPECT_RATIO,
                ),
            ) {
                BoxWithConstraints(contentAlignment = Alignment.Companion.BottomStart) {
                    AsyncImage(
                        model = item?.posterModel?.getCoilModel(
                            this.constraints.maxWidth,
                            this.constraints.maxHeight,
                        ),
                        modifier = Modifier.fillMaxSize(),
                        contentDescription = null,
                        contentScale = ContentScale.Companion.Crop,
                        fallback = item?.type?.icon?.let { rememberPlaceholderPainter(it, isError = false) },
                        error = item?.type?.icon?.let { rememberPlaceholderPainter(it, isError = true) },
                    )
                }
            }
        },
        overlineContent = {
            val scheme = item?.type?.colorScheme ?: MaterialTheme.colorScheme
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = scheme.secondaryContainer,
                contentColor = scheme.onSecondaryContainer,
            ) {
                Text(
                    text = item?.type?.itemType?.str().orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(vertical = 2.dp, horizontal = 6.dp),
                )
            }
        },
        content = { Text(item?.title.orEmpty(), style = MaterialTheme.typography.titleMedium) },
        supportingContent = {
            TagsRow(item?.tags.orEmpty())
        },
        trailingContent = {
            if (item?.trailingInfo != null) {
                Text(item.trailingInfo)
            }
        },
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
        shapes = ListItemShapes(position),
    )
}
