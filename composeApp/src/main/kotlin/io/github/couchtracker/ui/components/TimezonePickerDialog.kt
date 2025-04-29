package io.github.couchtracker.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
                tonalElevation = DatePickerDefaults.TonalElevation,
            ) {
                Column {
                    TimezoneDialogHeader(
                        categoriesStack = categoriesStack,
                        navigateUp = { category ->
                            categoriesStack = categoriesStack.dropLastWhile { it != category }
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

private fun LazyListScope.timezoneStackHeader(
    categoriesStack: List<TimezoneItem.Category>,
    onCategorySelected: (TimezoneItem.Category) -> Unit,
) {
    items(categoriesStack.size) { categoryIndex ->
        val category = categoriesStack[categoryIndex]
        if (categoryIndex == 0) {
            IconButton({ onCategorySelected(categoriesStack.first()) }) {
                Icon(Icons.Default.Public, contentDescription = null)
            }
        } else {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
            TextButton(
                enabled = categoryIndex < categoriesStack.size - 1,
                onClick = {
                    onCategorySelected(category)
                },
                content = { Text(category.mainName()) },
                colors = ButtonDefaults.textButtonColors(disabledContentColor = ButtonDefaults.textButtonColors().contentColor),
            )
        }
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
        LaunchedEffect(Unit) {
            val idx = timezones.indexOfFirst { it.isSelected(selected) }
            scrollState.scrollToItem(
                index = idx.coerceAtLeast(0),
                scrollOffset = (avgItemHeight - h) / 2,
            )
        }
        LazyColumn(state = scrollState) {
            items(timezones) { timezone ->
                TimezoneItemListEntry(timezone, selected, select)
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TimezoneDialogHeader(
    categoriesStack: List<TimezoneItem.Category>,
    navigateUp: (TimezoneItem.Category) -> Unit,
) {
    Surface(Modifier.fillMaxWidth(), color = AlertDialogDefaults.containerColor) {
        TopAppBar(
            title = {
                LazyRow(verticalAlignment = Alignment.CenterVertically) {
                    timezoneStackHeader(categoriesStack, navigateUp)
                    if (categoriesStack.size <= 1) {
                        item {
                            Text(R.string.select_time_zone.str())
                        }
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = AlertDialogDefaults.containerColor),
        )
    }
}

@Composable
private fun TimezoneItemListEntry(
    timezone: TimezoneItem,
    selected: TimeZone?,
    select: (TimezoneItem) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = timezone.mainName(),
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
private fun TimezoneItem.mainName(): String {
    return when (this) {
        is TimezoneItem.Zone -> leafName
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
