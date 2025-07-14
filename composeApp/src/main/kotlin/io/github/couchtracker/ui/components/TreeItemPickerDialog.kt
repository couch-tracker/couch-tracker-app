package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.utils.str

private typealias CategoriesStack<T> = List<TreeNode.Internal<T>>

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <T : Any> TreeItemPickerDialog(
    selected: T?,
    onSelect: (T?) -> Unit,
    root: TreeNode.Root<T>,
    itemKey: (T) -> Any,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    onClose: () -> Unit,
) {
    var categoriesStack: CategoriesStack<T> by remember { mutableStateOf(listOf(root)) }
    BasicAlertDialog(
        onDismissRequest = onClose,
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column {
                    TreeItemHeader(
                        icon = icon,
                        title = title,
                        itemKey = itemKey,
                        categoriesStack = categoriesStack,
                        navigateUp = { category ->
                            categoriesStack = categoriesStack.dropLastWhile { it != category }
                        },
                        selected = selected,
                        onSelect = {
                            onSelect(it)
                            onClose()
                        },
                    )
                    TreeItemsList(
                        modifier = Modifier.weight(1f, fill = false),
                        categoriesStack = categoriesStack,
                        selected = selected,
                        onSelect = {
                            when (it) {
                                is TreeNode.Category -> categoriesStack += it
                                is TreeNode.Item -> {
                                    onSelect(it.value)
                                    onClose()
                                }
                            }
                        },
                    )
                    FlowRow(
                        Modifier
                            .align(Alignment.End)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton({ onClose() }) { Text(R.string.cancel_action.str()) }
                        TextButton(
                            {
                                onSelect(null)
                                onClose()
                            },
                        ) {
                            Text(R.string.no_timezone.str()) // TODO
                        }
                    }
                }
            }
        },
    )
}

sealed interface TreeNode<out T : Any> {

    /**
     * Any node that is not the root node
     */
    sealed interface NonRoot<out T : Any> : TreeNode<T> {
        /**
         * Name to be used when navigating the tree
         */
        val name: @Composable () -> String

        /**
         * Optional subtitle (supporting) name
         */
        val supportingName: @Composable () -> String?

        /**
         * Name to use in the header breadcrumbs
         */
        val headerName: @Composable () -> String
    }

    /**
     * Any node that is not a leaf (i.e. has children)
     */
    sealed interface Internal<out T : Any> : TreeNode<T> {
        val children: List<NonRoot<T>>
    }

    data class Root<out T : Any>(
        override val children: List<NonRoot<T>>,
    ) : Internal<T>

    data class Category<out T : Any>(
        override val children: List<NonRoot<T>>,
        override val name: @Composable (() -> String),
        override val supportingName: @Composable (() -> String?),
        override val headerName: @Composable (() -> String) = name,
    ) : Internal<T>, NonRoot<T>

    data class Item<out T : Any>(
        val value: T,
        override val name: @Composable (() -> String),
        override val supportingName: @Composable (() -> String?),
        override val headerName: @Composable (() -> String),
        /**
         * Name to show on search results
         */
        val searchName: @Composable () -> String,
    ) : NonRoot<T>
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T : Any> TreeItemHeader(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    itemKey: (T) -> Any,
    categoriesStack: CategoriesStack<T>,
    navigateUp: (TreeNode.Internal<T>) -> Unit,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    var searchQueryField: String? by remember { mutableStateOf(null) }

    Surface(Modifier.fillMaxWidth(), color = AlertDialogDefaults.containerColor) {
        when (val query = searchQueryField) {
            null -> ItemPickerDialogTopAppBar(
                icon = icon,
                title = title,
                onOpenSearchBar = { searchQueryField = "" },
                categoriesStack = categoriesStack,
                navigateUp = navigateUp,
            )

            else -> ItemPickerDialogSearchBar(
                itemKey = itemKey,
                searchQuery = query,
                onSearchQuery = { searchQueryField = it },
                categoriesStack = categoriesStack,
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T : Any> ItemPickerDialogTopAppBar(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    categoriesStack: CategoriesStack<T>,
    navigateUp: (TreeNode.Internal<T>) -> Unit,
    onOpenSearchBar: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (val last = categoriesStack.last()) {
                    is TreeNode.Root -> {
                        IconButton({}) {
                            icon()
                        }
                        title()
                    }

                    is TreeNode.Category -> {
                        IconButton({ navigateUp(categoriesStack[categoriesStack.size - 2]) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                        Text(last.headerName())
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AlertDialogDefaults.containerColor),
        actions = {
            IconButton({ onOpenSearchBar() }) {
                Icon(Icons.Default.Search, contentDescription = R.string.search_action.str())
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <T : Any> ItemPickerDialogSearchBar(
    itemKey: (T) -> Any,
    searchQuery: String,
    onSearchQuery: (String?) -> Unit,
    categoriesStack: CategoriesStack<T>,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    var expanded by remember { mutableStateOf(true) }
    SearchBar(
        inputField = {
            SearchBarDefaults.InputField(
                query = searchQuery,
                onQueryChange = onSearchQuery,
                onSearch = { },
                expanded = expanded,
                onExpandedChange = { expanded = it },
                // TODO
                placeholder = {
                    Text(
                        R.string.timezone_search_placeholder.str(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                modifier = Modifier.focusRequester(focusRequester),
                trailingIcon = {
                    IconButton({ onSearchQuery(null) }) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                },
            )
        },
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        val searchResults = categoriesStack.last()
            .allChildren()
            .filter { it.matches(searchQuery) }
        LinearizedItemList(
            items = searchResults,
            itemKey = itemKey,
            itemName = { it.searchName() },
            selected = selected,
            onSelect = onSelect,
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun <T : Any> TreeItemsList(
    modifier: Modifier,
    categoriesStack: CategoriesStack<T>,
    selected: T?,
    onSelect: (TreeNode.NonRoot<T>) -> Unit,
) {
    val items = categoriesStack.last().children
    val scrollState = rememberLazyListState()
    BoxWithConstraints(modifier) {
        val h = this.constraints.maxHeight
        val avgItemHeight = LocalDensity.current.run { 72.dp.roundToPx() }
        LaunchedEffect(categoriesStack) {
            val idx = items.indexOfFirst { it.isSelected(selected) }
            scrollState.scrollToItem(
                index = idx.coerceAtLeast(0),
                scrollOffset = (avgItemHeight - h) / 2,
            )
        }
        LazyColumn(state = scrollState) {
            items(items) { item ->
                ItemListEntry(
                    item = item,
                    name = item.name(),
                    selected = selected,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@Composable
fun <T : Any> LinearizedItemList(
    items: List<TreeNode.Item<T>>,
    itemKey: (T) -> Any,
    itemName: @Composable (TreeNode.Item<T>) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    LazyColumn {
        items(items, key = { itemKey(it.value) }) { item ->
            ItemListEntry(
                item = item,
                name = itemName(item),
                selected = selected,
                onSelect = { onSelect(it.value) },
            )
        }
    }
}

@Composable
private fun <T : Any, TN : TreeNode.NonRoot<T>> ItemListEntry(
    item: TN,
    name: String,
    selected: T?,
    onSelect: (TN) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                fontWeight = if (item.isSelected(selected)) FontWeight.ExtraBold else null,
            )
        },
        supportingContent = item.supportingName()?.let { { Text(text = it) } },
        trailingContent = if (item is TreeNode.Category<*>) {
            {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        } else {
            null
        },
        modifier = Modifier.clickable { onSelect(item) },
        colors = ListItemDefaults.colors(
            containerColor = if (item.isSelected(selected)) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            },
        ),
    )
}

@Composable
private fun <T : Any> TreeNode.Item<T>.matches(searchQuery: String): Boolean {
    return name().contains(searchQuery, ignoreCase = true) ||
            supportingName()?.contains(searchQuery, ignoreCase = true) ?: false
}

private fun <T : Any> TreeNode.NonRoot<T>.isSelected(selected: T?): Boolean {
    return when (this) {
        is TreeNode.Category -> this.children.any { it.isSelected(selected) }
        is TreeNode.Item -> this.value == selected
    }
}

private fun <T : Any> TreeNode.Internal<T>.allChildren(): List<TreeNode.Item<T>> {
    return children.flatMap {
        when (it) {
            is TreeNode.Item -> listOf(it)
            is TreeNode.Category -> it.allChildren()
        }
    }
}
