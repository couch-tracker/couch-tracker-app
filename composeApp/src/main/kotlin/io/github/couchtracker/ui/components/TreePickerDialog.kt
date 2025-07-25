package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.HorizontalDivider
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
import io.github.couchtracker.utils.MixedValueTree
import io.github.couchtracker.utils.allLeafs
import io.github.couchtracker.utils.str

private typealias CategoriesStack<C, T> = List<MixedValueTree.Internal<C, T>>

/**
 * If these options are supplied to [TreePickerDialog], each tree branch will compute the suggested items with the [suggestedItems] lambda,
 * and shown before the full list.
 *
 * @param suggestedItems lambda to compute list of suggested items to show in the given tree branch
 * @param suggestedString composable lambda returning the name of the suggested category (e.g. "Suggested languages")
 * @param allString composable lambda returning the name of category with all the items, after the suggestions (e.g. "All languages")
 */
data class SuggestedOptions<C, T : Any>(
    val suggestedItems: (MixedValueTree.Internal<C, T>) -> List<MixedValueTree.Leaf<T>>,
    val suggestedString: @Composable () -> String,
    val allString: @Composable () -> String,
)

/**
 * Allows to pick an item out of a tree structure.
 *
 * @param selected the selected item, or `null` if there's nothing selected
 * @param onSelect callback with a new selection, or `null` if the user decided to select nothing
 * @param root root of the tree to show selections for this picker
 * @param suggestedOptions optional class containing parameters to compute the suggested items for each branch of the tree
 * @param itemName composable lambda returning the name of the item to show in the list
 * @param itemFullName optional composable lambda returning a full name for the item, to be used in search results. Defaults to [itemName]
 * @param itemSupportingName composable lambda optionally returning a string to be used in the supporting text of an item
 * @param itemKey lambda returning a key to use in a [LazyColumn] for the item
 * @param itemSearchStrings composable lambda returning a list of strings to be used to match against during the search.
 * Defaults to both [itemFullName] and [itemSupportingName]
 * @param categoryName composable lambda returning the name of the category to show in the list
 * @param categorySupportingName composable lambda optionally returning a string to be used in the supporting text of a category
 * @param icon the icon to use in the dialog header
 * @param title the title to use in the dialog header
 * @param searchPlaceHolder composable lambda returning the placeholder for the search text field
 * @param nullSelectionButtonText optional lambda returning a string for the dialog button to select a `null` item. If this lambda
 * is null, the button will not be visible, and users won't be able to select `null`.
 * @param onClose callback that should close the dialog
 * @param C the type of value held by categories
 * @param T the type of the items
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun <C, T : Any> TreePickerDialog(
    selected: T?,
    onSelect: (T?) -> Unit,
    root: MixedValueTree.Root<Unit, C, T>,
    suggestedOptions: SuggestedOptions<C, T>? = null,
    itemName: @Composable (T) -> String,
    itemFullName: @Composable (T) -> String = itemName,
    itemSupportingName: @Composable (T) -> String?,
    itemTrailingContent: (@Composable (T) -> Unit)? = null,
    itemKey: (T) -> Any,
    itemSearchStrings: @Composable (T) -> List<String> = { listOfNotNull(itemFullName(it), itemSupportingName(it)) },
    categoryName: @Composable (MixedValueTree.Intermediate<C, T>) -> String,
    categorySupportingName: @Composable (MixedValueTree.Intermediate<C, T>) -> String?,
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    searchPlaceHolder: @Composable () -> String,
    nullSelectionButtonText: (@Composable () -> String)?,
    onClose: () -> Unit,
) {
    var categoriesStack: CategoriesStack<C, T> by remember { mutableStateOf(listOf(root)) }
    val treePickerContext = TreePickerContext(
        itemName = itemName,
        itemFullName = itemFullName,
        itemSupportingName = itemSupportingName,
        itemTrailingContent = itemTrailingContent,
        itemKey = itemKey,
        itemSearchStrings = itemSearchStrings,
        categoryName = categoryName,
        categorySupportingName = categorySupportingName,
        searchPlaceHolder = searchPlaceHolder,
    )
    BasicAlertDialog(
        onDismissRequest = onClose,
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column {
                    with(treePickerContext) {
                        TreePickerDialogHeader(
                            icon = icon,
                            title = title,
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
                        TreePickerDialogItemsList(
                            modifier = Modifier.weight(1f, fill = false),
                            suggestedOptions = suggestedOptions,
                            categoriesStack = categoriesStack,
                            selected = selected,
                            onSelect = {
                                when (it) {
                                    is MixedValueTree.Intermediate -> categoriesStack += it
                                    is MixedValueTree.Leaf -> {
                                        onSelect(it.value)
                                        onClose()
                                    }
                                }
                            },
                        )
                    }
                    FlowRow(
                        Modifier
                            .align(Alignment.End)
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    ) {
                        TextButton({ onClose() }) { Text(R.string.cancel_action.str()) }
                        if (nullSelectionButtonText != null) {
                            TextButton(
                                {
                                    onSelect(null)
                                    onClose()
                                },
                            ) {
                                Text(nullSelectionButtonText())
                            }
                        }
                    }
                }
            }
        },
    )
}

private data class TreePickerContext<C, T : Any>(
    private val itemName: @Composable (T) -> String,
    private val itemSupportingName: @Composable (T) -> String?,
    private val itemFullName: @Composable (T) -> String,
    val itemTrailingContent: (@Composable (T) -> Unit)?,
    private val itemKey: (T) -> Any,
    private val itemSearchStrings: @Composable (T) -> List<String>,
    private val categoryName: @Composable (MixedValueTree.Intermediate<C, T>) -> String,
    private val categorySupportingName: @Composable (MixedValueTree.Intermediate<C, T>) -> String?,
    val searchPlaceHolder: @Composable (() -> String),
) {

    val MixedValueTree.Leaf<T>.trailingContent
        get() = itemTrailingContent?.let {
            @Composable {
                it(this.value)
            }
        }

    @Composable
    fun MixedValueTree.NonRoot<C, T>.name(): String {
        return when (this) {
            is MixedValueTree.Leaf -> itemName(this.value)
            is MixedValueTree.Intermediate -> categoryName(this)
        }
    }

    @Composable
    fun MixedValueTree.NonRoot<C, T>.supportingName(): String? {
        return when (this) {
            is MixedValueTree.Leaf -> itemSupportingName(this.value)
            is MixedValueTree.Intermediate -> categorySupportingName(this)
        }
    }

    @Composable
    fun MixedValueTree.Leaf<T>.matches(searchQuery: String): Boolean {
        return itemSearchStrings(value).any { it.contains(searchQuery, ignoreCase = true) }
    }

    @Composable
    fun MixedValueTree.Leaf<T>.fullName() = itemFullName(this.value)

    fun MixedValueTree.Leaf<T>.key() = itemKey(this.value)

    fun <C, T> MixedValueTree.NonRoot<C, T>.isSelected(selected: T?): Boolean {
        return when (this) {
            is MixedValueTree.Internal<*, *> -> this.children.any { it.isSelected(selected) }
            is MixedValueTree.Leaf -> this.value == selected
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun <C, T : Any> TreePickerContext<C, T>.TreePickerDialogHeader(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    categoriesStack: CategoriesStack<C, T>,
    navigateUp: (MixedValueTree.Internal<C, T>) -> Unit,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    var searchQueryField: String? by remember { mutableStateOf(null) }

    Surface(Modifier.fillMaxWidth(), color = AlertDialogDefaults.containerColor) {
        when (val query = searchQueryField) {
            null -> TreePickerDialogTopAppBar(
                icon = icon,
                title = title,
                onOpenSearchBar = { searchQueryField = "" },
                categoriesStack = categoriesStack,
                navigateUp = navigateUp,
            )

            else -> TreePickerDialogSearchBar(
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
private fun <C, T : Any> TreePickerContext<C, T>.TreePickerDialogTopAppBar(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    categoriesStack: CategoriesStack<C, T>,
    navigateUp: (MixedValueTree.Internal<C, T>) -> Unit,
    onOpenSearchBar: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                when (val last = categoriesStack.last()) {
                    is MixedValueTree.Root<*, *, *> -> {
                        IconButton({}) {
                            icon()
                        }
                        title()
                    }

                    is MixedValueTree.Intermediate -> {
                        IconButton({ navigateUp(categoriesStack[categoriesStack.size - 2]) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        }
                        Text(last.name())
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
private fun <C, T : Any> TreePickerContext<C, T>.TreePickerDialogSearchBar(
    searchQuery: String,
    onSearchQuery: (String?) -> Unit,
    categoriesStack: CategoriesStack<C, T>,
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
                placeholder = {
                    Text(
                        text = searchPlaceHolder(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
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
            .allLeafs()
            .toList()
            .filter { it.matches(searchQuery) }
        TreePickerDialogLinearizedItemList(
            items = searchResults,
            itemName = { it.fullName() },
            selected = selected,
            onSelect = onSelect,
        )
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun <C, T : Any> TreePickerContext<C, T>.TreePickerDialogItemsList(
    modifier: Modifier,
    suggestedOptions: SuggestedOptions<C, T>?,
    categoriesStack: CategoriesStack<C, T>,
    selected: T?,
    onSelect: (MixedValueTree.NonRoot<C, T>) -> Unit,
) {
    val node = categoriesStack.last()
    val items = node.children
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
            if (suggestedOptions != null) {
                val suggestedItems = suggestedOptions.suggestedItems(node)
                if (suggestedItems.isNotEmpty()) {
                    item("suggested-label") {
                        ListHeader(suggestedOptions.suggestedString())
                    }
                    items(suggestedItems, key = { "suggested-${it.value}" }) { item ->
                        TreePickerDialogItemListEntry(
                            item = item,
                            name = item.name(),
                            selected = selected,
                            onSelect = onSelect,
                        )
                    }
                    item(key = "divider") {
                        HorizontalDivider()
                    }
                    item(key = "all-label") {
                        Spacer(Modifier.size(16.dp))
                        ListHeader(suggestedOptions.allString())
                    }
                }
            }

            items(items) { item ->
                TreePickerDialogItemListEntry(
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
private fun ListHeader(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(horizontal = 16.dp),
        style = MaterialTheme.typography.labelLarge,
    )
}

@Composable
private fun <C, T : Any> TreePickerContext<C, T>.TreePickerDialogLinearizedItemList(
    items: List<MixedValueTree.Leaf<T>>,
    itemName: @Composable (MixedValueTree.Leaf<T>) -> String,
    selected: T?,
    onSelect: (T) -> Unit,
) {
    LazyColumn {
        items(items, key = { it.key() }) { item ->
            TreePickerDialogItemListEntry(
                item = item,
                name = itemName(item),
                selected = selected,
                onSelect = { onSelect(it.value) },
            )
        }
    }
}

@Composable
private fun <C, T : Any, TN : MixedValueTree.NonRoot<C, T>> TreePickerContext<C, T>.TreePickerDialogItemListEntry(
    item: TN,
    name: String,
    selected: T?,
    onSelect: (TN) -> Unit,
) {
    fun MixedValueTree.NonRoot<C, T>.trailingContent(): (@Composable () -> Unit)? {
        return when (this) {
            is MixedValueTree.Leaf -> this.trailingContent

            is MixedValueTree.Internal<*, *> -> {
                @Composable {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
                }
            }
        }
    }

    ListItem(
        headlineContent = {
            Text(
                text = name,
                fontWeight = if (item.isSelected(selected)) FontWeight.ExtraBold else null,
            )
        },
        supportingContent = item.supportingName()?.let { { Text(text = it) } },
        trailingContent = item.trailingContent(),
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
