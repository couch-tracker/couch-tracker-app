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

private data class CategoriesStack<I : Any, CATEGORY : I>(
    val root: List<I>,
    val stack: List<CATEGORY>,
) {
    fun pop() = copy(stack = stack.dropLast(1))

    fun peek() = stack.last()

    fun isRoot() = stack.isEmpty()

    fun children(treeVisitor: TreeVisitor<I, CATEGORY, *>): List<I> {
        return if (isRoot()) {
            root
        } else {
            treeVisitor.children(peek())
        }
    }

    operator fun plus(category: CATEGORY) = copy(stack = stack + category)
}

internal data class TreeVisitor<BASE : Any, CATEGORY : BASE, LEAF : BASE>(
    val castToCategory: BASE.() -> CATEGORY?,
    val castToLeaf: BASE.() -> LEAF?,
    val children: CATEGORY.() -> List<BASE>,
    val name: @Composable BASE.() -> String,
    val supportingName: @Composable BASE.() -> String?,
    val searchName: @Composable LEAF.() -> String,
    val headerName: @Composable CATEGORY.() -> String,
    val key: LEAF.() -> Any,
) {

    fun <R> BASE.exhaustiveWhen(onCategory: (CATEGORY) -> R, onLeaf: (LEAF) -> R): R {
        val category = castToCategory()
        val leaf = castToLeaf()
        return if (category != null) {
            onCategory(category)
        } else if (leaf != null) {
            onLeaf(leaf)
        } else {
            error("Type ${this::class} is neither a category nor a leaf")
        }
    }

    fun BASE.allLeafs(): List<LEAF> {
        return exhaustiveWhen(
            onLeaf = { listOf(it) },
            onCategory = { category ->
                category.children().flatMap { it.allLeafs() }
            },
        )
    }

    @Composable
    fun LEAF.matches(searchQuery: String): Boolean {
        return name().contains(searchQuery, ignoreCase = true) ||
                supportingName()?.contains(searchQuery, ignoreCase = true) ?: false
    }

    fun BASE.isSelected(selected: LEAF?): Boolean {
        return exhaustiveWhen(
            onCategory = { it.children().any { child -> child.isSelected(selected) } },
            onLeaf = { it == selected },
        )
    }
}

@Composable
internal inline fun <BASE : Any, reified CATEGORY : BASE, reified LEAF : BASE> TreeItemPickerDialog(
    // Tree visitor stuff
    root: List<BASE>,
    noinline categoryChildren: (CATEGORY) -> List<BASE>,
    noinline name: @Composable (BASE) -> String,
    noinline supportingName: @Composable (BASE) -> String?,
    noinline searchName: @Composable (LEAF) -> String,
    noinline headerName: @Composable (CATEGORY) -> String,
    noinline leafKey: (LEAF) -> Any,

    // Composable stuff
    crossinline icon: @Composable () -> Unit,
    crossinline title: @Composable () -> Unit,
    selected: LEAF?,
    crossinline onSelect: (LEAF?) -> Unit,
    crossinline onClose: () -> Unit,
) {
    val treeVisitor = remember(categoryChildren, name, supportingName, searchName, headerName, leafKey) {
        TreeVisitor(
            castToCategory = { this as? CATEGORY },
            castToLeaf = { this as? LEAF },
            children = categoryChildren,
            name = name,
            supportingName = supportingName,
            searchName = searchName,
            headerName = headerName,
            key = leafKey,
        )
    }
    treeVisitor.TreeItemPickerDialog(
        root = root,
        icon = { icon() },
        title = { title() },
        selected = selected,
        onSelect = { onSelect(it) },
        onClose = { onClose() },
    )
}


@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.TreeItemPickerDialog(
    root: List<BASE>,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    selected: LEAF?,
    onSelect: (LEAF?) -> Unit,
    onClose: () -> Unit,
) {
    var categoriesStack by remember { mutableStateOf(CategoriesStack(root, emptyList<CATEGORY>())) }
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
                        categoriesStack = categoriesStack,
                        navigateTo = { categoriesStack = it },
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
                        onSelect = { item ->
                            item.exhaustiveWhen(
                                onCategory = { categoriesStack += it },
                                onLeaf = {
                                    onSelect(it)
                                    onClose()
                                },
                            )
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

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.TreeItemHeader(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    categoriesStack: CategoriesStack<BASE, CATEGORY>,
    navigateTo: (CategoriesStack<BASE, CATEGORY>) -> Unit,
    selected: LEAF?,
    onSelect: (LEAF?) -> Unit,
) {
    var searchQueryField: String? by remember { mutableStateOf(null) }

    Surface(Modifier.fillMaxWidth(), color = AlertDialogDefaults.containerColor) {
        when (val query = searchQueryField) {
            null -> ItemPickerDialogTopAppBar(
                icon = icon,
                title = title,
                onOpenSearchBar = { searchQueryField = "" },
                categoriesStack = categoriesStack,
                navigateTo = navigateTo,
            )

            else -> ItemPickerDialogSearchBar(
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
private fun <I : Any, CATEGORY : I> TreeVisitor<I, CATEGORY, *>.ItemPickerDialogTopAppBar(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    categoriesStack: CategoriesStack<I, CATEGORY>,
    navigateTo: (CategoriesStack<I, CATEGORY>) -> Unit,
    onOpenSearchBar: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (categoriesStack.isRoot()) {
                    IconButton({}) {
                        icon()
                    }
                    title()
                } else {
                    IconButton({ navigateTo(categoriesStack.pop()) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    Text(categoriesStack.peek().headerName())
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
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.ItemPickerDialogSearchBar(
    searchQuery: String,
    onSearchQuery: (String?) -> Unit,
    categoriesStack: CategoriesStack<BASE, CATEGORY>,
    selected: LEAF?,
    onSelect: (LEAF?) -> Unit,
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
                placeholder = { Text(R.string.timezone_search_placeholder.str(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
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
        val items = if (categoriesStack.isRoot()) {
            categoriesStack.root
        } else {
            listOf(categoriesStack.peek())
        }
        val searchResults = items
            .flatMap { it.allLeafs() }
            .filter { it.matches(searchQuery) }

        LinearizedItemList(
            items = searchResults,
            itemName = searchName,
            selected = selected,
            onSelect = onSelect,
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.TreeItemsList(
    modifier: Modifier,
    categoriesStack: CategoriesStack<BASE, CATEGORY>,
    selected: LEAF?,
    onSelect: (BASE) -> Unit,
) {
    val items = categoriesStack.children(this)
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
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.LinearizedItemList(
    items: List<LEAF>,
    itemName: @Composable LEAF.() -> String,
    selected: LEAF?,
    onSelect: (LEAF) -> Unit,
) {
    LazyColumn {
        items(items, key = { it.key() }) { item ->
            ItemListEntry(
                item = item,
                name = item.itemName(),
                selected = selected,
                onSelect = onSelect,
            )
        }
    }
}

@Composable
private fun <BASE : Any, CATEGORY : BASE, LEAF : BASE, I : BASE> TreeVisitor<BASE, CATEGORY, LEAF>.ItemListEntry(
    item: I,
    name: String,
    selected: LEAF?,
    onSelect: (I) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = name,
                fontWeight = if (item.isSelected(selected)) FontWeight.ExtraBold else null,
            )
        },
        supportingContent = item.supportingName()?.let { { Text(text = it) } },
        trailingContent = if (item.castToCategory() != null) {
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
