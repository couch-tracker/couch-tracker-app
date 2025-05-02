package io.github.couchtracker.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.github.couchtracker.R
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.str
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@Composable
fun <T : Any> PaginatedGrid(
    paginatedItems: LazyPagingItems<T>,
    columns: GridCells,
    itemComposable: @Composable (T?) -> Unit,
) {
    val globalState = when (paginatedItems.loadState.refresh) {
        is LoadState.Error -> Loadable.Error(R.string.error_loading_items.str())
        LoadState.Loading -> Loadable.Loading
        is LoadState.NotLoading -> Loadable.Loaded(Unit)
    }
    LoadableScreen(
        globalState,
        onError = {
            DefaultErrorScreen(it) { paginatedItems.retry() }
        },
    ) {
        LoadedPaginatedGrid(paginatedItems, columns, itemComposable)
    }
}

private enum class ItemTypes {
    ITEM,
    ERROR,
}

@Composable
private fun <T : Any> LoadedPaginatedGrid(
    paginatedItems: LazyPagingItems<T>,
    columns: GridCells,
    itemComposable: @Composable (T?) -> Unit,
) {
    val bottomState = paginatedItems.loadState.append
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        columns = columns,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        val snapshot = paginatedItems.itemSnapshotList
        items(
            count = if (bottomState is LoadState.Error) {
                snapshot.items.size + 1
            } else {
                snapshot.size
            },
            span = { index ->
                val fullWidth = bottomState is LoadState.Error && paginatedItems[index] == null
                GridItemSpan(if (fullWidth) maxLineSpan else 1)
            },
            contentType = { index ->
                val item = paginatedItems[index]
                if (isErrorView(item, bottomState)) {
                    ItemTypes.ERROR
                } else {
                    ItemTypes.ITEM
                }
            },
        ) { index ->
            val item = paginatedItems[index]
            if (isErrorView(item, bottomState)) {
                ErrorMessageComposable(
                    Modifier.fillMaxWidth(),
                    R.string.error_loading_items.str(),
                    retry = { paginatedItems.retry() },
                )
            } else {
                Box(Modifier.animateContentSize()) {
                    itemComposable(item)
                }
            }
        }
    }
}

@OptIn(ExperimentalContracts::class)
private fun <T : Any> isErrorView(item: T?, bottomState: LoadState): Boolean {
    contract {
        returns(true) implies (bottomState is LoadState.Error)
        returns(true) implies (item == null)
    }
    return item == null && bottomState is LoadState.Error
}
