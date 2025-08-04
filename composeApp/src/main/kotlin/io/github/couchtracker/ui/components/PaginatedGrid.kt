package io.github.couchtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import io.github.couchtracker.R
import io.github.couchtracker.utils.ApiException
import io.github.couchtracker.utils.Loadable
import io.github.couchtracker.utils.Result
import io.github.couchtracker.utils.isLoaded
import io.github.couchtracker.utils.str
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

private typealias ErrorWithDetails = Pair<String, String?>

@Composable
fun <T : Any> PaginatedGrid(
    paginatedItems: LazyPagingItems<T>,
    columns: GridCells,
    contentPadding: PaddingValues = PaddingValues(8.dp),
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(8.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(16.dp),
    emptyComposable: (@Composable () -> Unit)? = null,
    lazyGridState: LazyGridState = rememberLazyGridState(),
    itemComposable: @Composable (T?) -> Unit,
) {
    val globalState = when (val state = paginatedItems.loadState.refresh) {
        is LoadState.Error -> Result.Error(state.error.wrap())
        LoadState.Loading -> Loadable.Loading
        is LoadState.NotLoading -> Result.Value(Unit)
    }
    LoadableScreen(
        globalState,
        onError = { (message, details) ->
            DefaultErrorScreen(errorMessage = message, errorDetails = details, retry = { paginatedItems.retry() })
        },
    ) {
        if (paginatedItems.loadState.isLoaded && paginatedItems.itemSnapshotList.isEmpty() && emptyComposable != null) {
            emptyComposable()
        } else {
            LoadedPaginatedGrid(
                paginatedItems = paginatedItems,
                columns = columns,
                contentPadding = contentPadding,
                horizontalArrangement = horizontalArrangement,
                verticalArrangement = verticalArrangement,
                lazyGridState = lazyGridState,
                itemComposable = itemComposable,
            )
        }
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
    contentPadding: PaddingValues,
    horizontalArrangement: Arrangement.Horizontal,
    verticalArrangement: Arrangement.Vertical,
    lazyGridState: LazyGridState,
    itemComposable: @Composable (T?) -> Unit,
) {
    val bottomState = paginatedItems.loadState.append
    LazyVerticalGrid(
        modifier = Modifier.fillMaxSize(),
        state = lazyGridState,
        columns = columns,
        contentPadding = contentPadding,
        horizontalArrangement = horizontalArrangement,
        verticalArrangement = verticalArrangement,
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
                val wrappedError = bottomState.error.wrap()
                ErrorMessageComposable(
                    modifier = Modifier.fillMaxWidth(),
                    errorMessage = wrappedError.first,
                    errorDetails = wrappedError.second,
                    retry = { paginatedItems.retry() },
                )
            } else {
                itemComposable(item)
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

@Composable
private fun Throwable.wrap(): ErrorWithDetails {
    return if (this is ApiException) {
        R.string.error_loading_items_x.str(title.string()) to details?.string()
    } else {
        R.string.error_loading_items.str() to null
    }
}
