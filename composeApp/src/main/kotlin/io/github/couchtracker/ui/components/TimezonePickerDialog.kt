package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Public
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.couchtracker.R
import io.github.couchtracker.utils.TimezoneItem
import io.github.couchtracker.utils.currentFirstLocale
import io.github.couchtracker.utils.pluralStr
import io.github.couchtracker.utils.str
import io.github.couchtracker.utils.timezonesTree
import kotlinx.datetime.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimezonePickerDialog(
    timezone: TimeZone?,
    onTimezoneSelected: (TimeZone) -> Unit,
    close: () -> Unit,
) {
    val timezonesTreeRoot = remember { TimeZone.timezonesTree() }
    var categoriesStack: List<TimezoneItem.Category> by remember { mutableStateOf(listOf(timezonesTreeRoot)) }
    BasicAlertDialog(
        onDismissRequest = close,
        content = {
            Surface(
                shape = AlertDialogDefaults.shape,
                color = AlertDialogDefaults.containerColor,
                tonalElevation = AlertDialogDefaults.TonalElevation,
            ) {
                Column {
                    TimeZoneHeader(
                        categoriesStack = categoriesStack,
                        navigateUp = { category ->
                            categoriesStack = categoriesStack.dropLastWhile { it != category }
                        },
                        selected = timezone,
                        select = {
                            onTimezoneSelected(it.timezone)
                            close()
                        },
                    )
                    TimeZoneItemsList(
                        categoriesStack = categoriesStack,
                        selected = timezone,
                        select = {
                            when (it) {
                                is TimezoneItem.Category -> categoriesStack = categoriesStack.plus(it)
                                is TimezoneItem.Zone -> {
                                    onTimezoneSelected(it.timezone)
                                    close()
                                }
                            }
                        },
                    )
                }
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeZoneHeader(
    categoriesStack: List<TimezoneItem.Category>,
    navigateUp: (TimezoneItem.Category) -> Unit,
    selected: TimeZone?,
    select: (TimezoneItem.Zone) -> Unit,
) {
    var searchQueryField: String? by remember { mutableStateOf(null) }

    Surface(Modifier.fillMaxWidth(), color = AlertDialogDefaults.containerColor) {
        when (val sf = searchQueryField) {
            null -> TimeZoneTopAppBar(
                openSearchBar = { searchQueryField = "" },
                categoriesStack = categoriesStack,
                navigateUp = navigateUp,
            )

            else -> TimeZoneSearchBar(
                searchQuery = sf,
                onSearchQuery = { searchQueryField = it },
                categoriesStack = categoriesStack,
                selected = selected,
                select = select,
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeZoneTopAppBar(
    openSearchBar: () -> Unit,
    categoriesStack: List<TimezoneItem.Category>,
    navigateUp: (TimezoneItem.Category) -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (categoriesStack.size <= 1) {
                    IconButton({}) {
                        Icon(Icons.Default.Public, contentDescription = null)
                    }
                    Text(R.string.select_time_zone.str())
                } else {
                    IconButton({ navigateUp(categoriesStack[categoriesStack.size - 2]) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                    Text(categoriesStack.last().mainName(false))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = AlertDialogDefaults.containerColor),
        actions = {
            IconButton({ openSearchBar() }) {
                Icon(Icons.Default.Search, contentDescription = R.string.search_action.str())
            }
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimeZoneSearchBar(
    searchQuery: String,
    onSearchQuery: (String?) -> Unit,
    categoriesStack: List<TimezoneItem.Category>,
    selected: TimeZone?,
    select: (TimezoneItem.Zone) -> Unit,
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
        val searchResults = categoriesStack.last()
            .allTimeZones()
            .filter { it.matches(searchQuery) }
        LinearizedTimezoneList(searchResults, selected, select)
    }
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TimeZoneItemsList(
    categoriesStack: List<TimezoneItem.Category>,
    selected: TimeZone?,
    select: (TimezoneItem) -> Unit,
) {
    val timezones = categoriesStack.last().items
    val scrollState = rememberLazyListState()
    BoxWithConstraints {
        val h = this.constraints.maxHeight
        val avgItemHeight = LocalDensity.current.run { 72.dp.roundToPx() }
        LaunchedEffect(categoriesStack) {
            val idx = timezones.indexOfFirst { it.isSelected(selected) }
            scrollState.scrollToItem(
                index = idx.coerceAtLeast(0),
                scrollOffset = (avgItemHeight - h) / 2,
            )
        }
        LazyColumn(state = scrollState) {
            items(timezones) { timezone ->
                TimezoneItemListEntry(timezone, selected, select, fullName = false)
            }
        }
    }
}

@Composable
fun LinearizedTimezoneList(
    timezones: List<TimezoneItem.Zone>,
    selected: TimeZone?,
    select: (TimezoneItem.Zone) -> Unit,
) {
    LazyColumn {
        items(timezones, key = { it.timezone.id }) { timezone ->
            TimezoneItemListEntry(timezone, selected, select, fullName = true)
        }
    }
}

@Composable
private fun <TZ : TimezoneItem> TimezoneItemListEntry(
    timezone: TZ,
    selected: TimeZone?,
    select: (TZ) -> Unit,
    fullName: Boolean,
) {
    ListItem(
        headlineContent = {
            Text(
                text = timezone.mainName(fullName),
                fontWeight = if (timezone.isSelected(selected)) FontWeight.ExtraBold else null,
            )
        },
        supportingContent = { Text(text = timezone.supportingName()) },
        trailingContent = if (timezone is TimezoneItem.Category) {
            {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            }
        } else {
            null
        },
        modifier = Modifier.clickable { select(timezone) },
        colors = ListItemDefaults.colors(
            containerColor = if (timezone.isSelected(selected)) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            },
        ),
    )
}

@Composable
private fun TimezoneItem.Zone.matches(searchQuery: String): Boolean {
    return mainName(fullName = true).contains(searchQuery, ignoreCase = true) ||
        supportingName().contains(searchQuery, ignoreCase = true)
}

@Composable
private fun TimezoneItem.mainName(fullName: Boolean): String {
    return when (this) {
        is TimezoneItem.Zone -> if (fullName) timezone.id else leafName
        is TimezoneItem.Category.Root -> error("Root doesn't have a name")
        is TimezoneItem.Category.GeographicalArea -> this.name
        is TimezoneItem.Category.Uncategorized -> R.string.timezone_category_other.str()
    }
}

@Composable
private fun TimezoneItem.supportingName(): String {
    return when (this) {
        is TimezoneItem.Category -> {
            val cnt = totalItemCount()
            R.plurals.timezone_category_subtitle.pluralStr(cnt, cnt)
        }

        is TimezoneItem.Zone -> {
            val androidTz = android.icu.util.TimeZone.getFrozenTimeZone(timezone.id)
            androidTz.getDisplayName(LocalConfiguration.currentFirstLocale)
        }
    }
}

private fun TimezoneItem.isSelected(selectedTz: TimeZone?): Boolean {
    return when (this) {
        is TimezoneItem.Category -> items.any { it.isSelected(selectedTz) }
        is TimezoneItem.Zone -> selectedTz == timezone
    }
}
